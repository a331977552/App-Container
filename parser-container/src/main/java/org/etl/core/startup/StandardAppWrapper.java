package org.etl.core.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.AppWrapper;
import org.etl.core.FileMonitorService;
import org.etl.core.loader.AppClassLoader;
import org.etl.core.loader.AppLoader;
import org.etl.core.loader.Loader;
import org.etl.service.ReferenceServiceImpl;
import org.etl.service.ReferenceServiceReceiver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

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

    private Class<?> appClass;
    private boolean reloadable = false;
    private String absAppPath;


    private String appMountPath;

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
        this.getLoader().close();
        AppLoader appLoader = new AppLoader();
        appLoader.setAppWrapper(this);
        appLoader.setAppClassLoader(new AppClassLoader(this.getAppMountPath(),getName()));
        setLoader(appLoader);
        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setPause(false);
    }

    @Override
    public void start() throws Exception {
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
        if (fileMonitorService!=null){
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

    private void internalStart() throws ClassNotFoundException {
        //todo main class configurable like reading from manifest
        ClassLoader parserServiceClassLoader = getLoader().getClassLoader();
        appClass = parserServiceClassLoader.loadClass("org.etl.app.App");
        new Thread(() -> {
            try {
                Class<? extends ReferenceServiceReceiver> subclass = appClass.asSubclass(ReferenceServiceReceiver.class);
                Method main = appClass.getMethod("main", String[].class);
                ReferenceServiceReceiver referenceServiceReceiver = subclass.getConstructor().newInstance();
                ReferenceServiceImpl referenceService = new ReferenceServiceImpl();
                referenceServiceReceiver.onReferenceServiceArrived(referenceService);
                main.invoke(null, (Object) new String[]{});
            } catch (InstantiationException | NoSuchMethodException | IllegalAccessException |
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

        if (appClass != null) {
            Method stop = appClass.getMethod("stop", boolean.class);
            Object invoke = stop.invoke(null, forceStop);
            log.info("process {} has been stopped", appClass.getName());
        } else {
            log.warn("unable to find app with name :{} in app container", this.getName());
        }
    }


}
