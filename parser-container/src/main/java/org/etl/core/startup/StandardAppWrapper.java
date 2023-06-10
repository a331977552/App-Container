package org.etl.core.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.AppWrapper;
import org.etl.core.BaseLifeCycle;
import org.etl.core.FileMonitor;
import org.etl.core.exception.LifecycleException;
import org.etl.core.loader.AppClassLoader;
import org.etl.core.loader.AppLoader;
import org.etl.core.loader.Loader;
import org.etl.service.Application;
import org.etl.service.Context;
import org.etl.service.ContextFacade;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;

@Slf4j
public class StandardAppWrapper extends BaseLifeCycle implements AppWrapper {

    private String name;
    private Loader loader;

    private FileMonitor fileMonitor;

    /**
     * it might store a jar file or a folder
     */
    SynchronousQueue<File> appFile = new SynchronousQueue<>(true);

    private Class<? extends Application> appClass;
    private boolean reloadable = false;
    private final String absAppPath;

    private String appMountPath;
    private Application application;
    private Context context;

    public StandardAppWrapper(String absAppPath) {
        this.absAppPath = absAppPath;
        Objects.requireNonNull(absAppPath);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean getReloadable() {
        return reloadable;
    }

    @Override
    public String getAbsAppPath() {
        return this.absAppPath;
    }

    @Override
    public String getAppMountPath() {
        return this.appMountPath;
    }

    protected void setApplication(Application application) {
        this.application = application;
    }

    @Override
    public Application getApplicationInstance() {
        return application;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    public void setAppMountPath(String appMountPath) {
        this.appMountPath = appMountPath;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    @Override
    public void reload() {
        try {
            stop();
        } catch (LifecycleException e) {
            log.error("unable to stop component", e);
            return;
        }
        AppLoader appLoader = new AppLoader();
        appLoader.setAppWrapper(this);
        appLoader.setAppClassLoader(new AppClassLoader(this.getAppMountPath(), getName()));
        setLoader(appLoader);
        try {
            start();
        } catch (LifecycleException e) {
            log.error("unable to start component", e);
        }
    }



    private void backgroundProcess() {
        if (!getReloadable()) {
            return;
        }
        //1.find jar and
        //2. todo verify jar
        //3. stop existing service
        //4. delete folder
        //5. unzip jar
        //6. delete jar
        //7. start new service
        // else
        // 2.find service folder
        // 3.stop existing service
        // 4.start new service
        //todo check all subdirectories for this app
        if (fileMonitor == null) {
            fileMonitor = new FileMonitor(new File(getAbsAppPath()), pathname -> true);
            fileMonitor.setFileAlterationListenerAdaptor(new FileAlterationListenerAdaptor() {
                @Override
                public void onFileChange(File file) {
                    log.info("file {} modified, restarting app {}", file.getAbsolutePath(), getName());
                    Thread currentThread = Thread.currentThread();
                    ClassLoader originalTccl = currentThread.getContextClassLoader();
                    try {
                        currentThread.setContextClassLoader(AppLoader.class.getClassLoader());
                        reload();
                    } finally {
                        currentThread.setContextClassLoader(originalTccl);
                    }
                }
            });
        }
        try {
            fileMonitor.start();
        } catch (Exception e) {
            log.error("unable to monitor app" + this.getName(), e);
        }
    }
    @Override
    protected void internalInit() throws LifecycleException {
        if (getLoader() == null || getAppMountPath() == null){
            throw new LifecycleException("unable to initializing app wrapper "+ this);
        }
    }

    @Override
    public void internalStart() {
        loadApp();
        backgroundProcess();
    }
    @Override
    protected void internalStop() throws LifecycleException {
        if (fileMonitor != null) {
            fileMonitor.stop();
        }
        if (getApplicationInstance() != null) {
            log.info("try to stop app: {}", appClass.getName());
            getApplicationInstance().stop(false);
            getLoader().close();
            log.info("app {} has been stopped", appClass.getName());
        } else {
            log.warn("unable to find app with name :{} in app container", this.getName());
        }
    }

    @Override
    protected void internalDestroy() throws LifecycleException {
        fileMonitor.stop();
        fileMonitor.setFileAlterationListenerAdaptor(null);
        fileMonitor = null;
        appClass = null;
    }


    @Override
    public void setLoader(Loader loader) {
        this.loader = loader;
    }

    @Override
    public Loader getLoader() {
        return loader;
    }



    private void loadApp() {
        Thread thread = new Thread(() -> {
            try {
                ClassLoader parserServiceClassLoader = getLoader().getClassLoader();
                //todo make the config.property location configurable
                URL resource = getLoader().getClassLoader().getResource("app/config.properties");
                String mainClass;
                try {
                    File path = Paths.get(resource.toURI()).toFile();
                    Properties properties = new Properties();
                    properties.load(new FileInputStream(path));
                    mainClass = (String) properties.get("main.class");
                } catch (URISyntaxException | IOException | NullPointerException e) {
                    log.error("unable to grable config.properties, please check if you config your app correctly", e);
                    return;
                }
                log.info("retrieved main class {}, starting", mainClass);
                appClass = parserServiceClassLoader.loadClass(mainClass).asSubclass(Application.class);
                Application application = appClass.getConstructor().newInstance();
                setApplication(application);
                application.start(new ContextFacade(context));
            } catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException |
                     InvocationTargetException e) {
                log.error("app " + getName() + " has run into exception, please deploy an valid app !!", e);
            }
        });
        thread.start();
    }


}
