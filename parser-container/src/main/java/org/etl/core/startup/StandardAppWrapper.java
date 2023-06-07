package org.etl.core.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.AppWrapper;
import org.etl.core.FileMonitorService;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;

@Slf4j
public class StandardAppWrapper implements AppWrapper {

    private String name;
    private Loader loader;
    private boolean pause;

    private FileMonitorService fileMonitorService;

    /**
     * it might store a jar file or a folder
     */
    SynchronousQueue<File> appFile = new SynchronousQueue<>(true);
    private boolean started;

    private Class<? extends Application> appClass;
    private boolean reloadable = false;
    private String absAppPath;


    private String appMountPath;
    private Application application;
    private Context context;

    public StandardAppWrapper(String absAppPath) {
        this.absAppPath = absAppPath;
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
        setPause(true);
        stop();
        AppLoader appLoader = new AppLoader();
        appLoader.setAppWrapper(this);
        appLoader.setAppClassLoader(new AppClassLoader(this.getAppMountPath(), getName()));
        setLoader(appLoader);
        start();
        setPause(false);
    }

    @Override
    public void start() {
        internalStart();
        backgroundProcess();
        this.started = true;
    }

    private void backgroundProcess() {
        if (getReloadable()) {
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
            if (fileMonitorService == null) {
                fileMonitorService = new FileMonitorService(new File(getAbsAppPath()), pathname -> true);
                fileMonitorService.setFileAlterationListenerAdaptor(new FileAlterationListenerAdaptor() {
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
                fileMonitorService.start();
            } catch (Exception e) {
                log.error("unable to monitor app" + this.getName(), e);
            }
        }

    }

    /**
     * stop all background process,
     * and close all resources.
     */
    @Override
    public void stop() {
        try {
            stopBackgroundProcess();
            stopInternal(false);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void stopBackgroundProcess() {
        if (fileMonitorService != null) {
            fileMonitorService.stop();
        }
    }

    @Override
    public boolean started() {
        return started;
    }

    private void setPause(boolean pause) {
        this.pause = pause;
        //todo stop client from accepting more feeds
    }

    @Override
    public void setLoader(Loader loader) {
        this.loader = loader;
    }

    @Override
    public Loader getLoader() {
        return loader;
    }

    private void internalStart() {

        new Thread(() -> {
            try {
                ClassLoader parserServiceClassLoader = getLoader().getClassLoader();
                //todo make the config.property location configurable
                URL resource = getLoader().getClassLoader().getResource("app/config.properties");
                Properties properties;
                try {
                    File path = Paths.get(resource.toURI()).toFile();
                    properties = new Properties();
                    properties.load(new FileInputStream(path));
                } catch (URISyntaxException | IOException|NullPointerException e) {
                    log.error("unable to grable config.properties, please check if you config your app correctly",e);
                    return;
                }
                String mainClass = (String) properties.get("main.class");
                log.info("retrieved main class {}, starting",mainClass);
                appClass = parserServiceClassLoader.loadClass(mainClass).asSubclass(Application.class);
                Application application = appClass.getConstructor().newInstance();
                setApplication(application);
                application.start(new ContextFacade(context));
            } catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException |
                     InvocationTargetException e) {
                log.error("app " + getName() + " has run into exception, please check !!", e);
                StandardAppWrapper.this.stop();
            }
        }).start();
    }


    /**
     * it will block until the service is finished
     */
    protected void stopInternal(boolean forceStop) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        if (getApplicationInstance() != null) {
            log.info("try to stop app: {}", appClass.getName());
            getApplicationInstance().stop(forceStop);
            getLoader().close();
            log.info("app {} has been stopped", appClass.getName());
        } else {
            log.warn("unable to find app with name :{} in app container", this.getName());
        }
    }


}
