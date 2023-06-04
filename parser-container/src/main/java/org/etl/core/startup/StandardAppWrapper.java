package org.etl.core.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.AppWrapper;
import org.etl.core.FileMonitorService;
import org.etl.core.loader.AppClassLoader;
import org.etl.core.loader.Loader;
import org.etl.service.ReferenceServiceImpl;
import org.etl.service.ReferenceServiceReceiver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static org.etl.core.startup.BootStrap.APP_HOME;

@Slf4j
public class StandardAppWrapper implements AppWrapper {

    private static final Object lock = new Object();
    private String name;
    private Loader loader;
    private boolean pause;


    /**
     * it might store a jar file or a folder
     */
    SynchronousQueue<File> appFile = new SynchronousQueue<>(true);
    private JarDeployer jarDeployer;
    private boolean started;
    private AppClassLoader parserServiceClassLoader;

    private Class<?> appClass;
    private boolean reloadable = false;

    public void setName(String name){
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean getReloadable() {
        return reloadable;
    }
    public void setReloadable(boolean reloadable){
        this.reloadable = reloadable;
    }

    @Override
    public void reload()  {
        setPause(true);

        stop();

        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setPause(false);
    }

    @Override
    public void start() throws Exception {
        //todo start client
        this.started = true;
            try {
                //todo get file by name();
                //1.check if there is jar exist in the folder, if it exists, explodes it
                //2.start the folder
                File file = appFile.poll(100, TimeUnit.MILLISECONDS);
                if (file == null)
                    return;
                if (file.isDirectory()) {
                    log.info("new app Directory found:{} trying to start", file.getName());
                    internalStart(file.getParentFile().getAbsolutePath(), file.getName());
                } else if (file.getName().endsWith(".jar")) {
                    log.info("new app jar found:{} trying to deploy and start", file.getName());
                    deployAndStart(file);
                } else {
                    log.warn("unrecognized file :{}", file);
                }
            } catch (InterruptedException ignored) {
            }

            backgroundProcess();
    }

    private void backgroundProcess() {
        this.getLoader().backgroundProcess();
    }

    @Override
    public void stop() {
        try {
            stopInternal(false);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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






    public void setJarDeployer(JarDeployer jarDeployer){
        this.jarDeployer = jarDeployer;
    }


    public JarDeployer getJarDeployer() {
        return jarDeployer;
    }

    private void deployAndStart(File jar) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (parserServiceClassLoader!=null){
            //call close to make sure all the previously loaded class files and jar files can be removed.
            parserServiceClassLoader.close();
        }
        jarDeployer.deleteAppFolder(this.name());
        jarDeployer.deploy(this.name(), jar);
        jarDeployer.deleteJar(jar);
        internalStart(jar.getParentFile().getAbsolutePath(), this.name());
    }

    private void internalStart(String rootPath, String appName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        parserServiceClassLoader = new AppClassLoader(rootPath, appName);

        //todo main class configurable like reading from manifest
        Class<?> appMain = parserServiceClassLoader.loadClass("org.etl.app.App");


        new Thread(() -> {
            try {
                String[] args = new String[]{};
                ClassLoader classLoader = appMain.getClassLoader();
                System.out.println(classLoader);
                System.out.println(ReferenceServiceReceiver.class.getClassLoader());

                Class<? extends ReferenceServiceReceiver> subclass = appMain.asSubclass(ReferenceServiceReceiver.class);
                Method main = appMain.getMethod("main", String[].class);
                ReferenceServiceReceiver referenceServiceReceiver = subclass.getConstructor().newInstance();
                ReferenceServiceImpl referenceService = new ReferenceServiceImpl();
                referenceServiceReceiver.onReferenceServiceArrived(referenceService);
                main.invoke(null,(Object) args);

            } catch (InstantiationException |NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("app "+appName+" has run into exception, please check !!",e);
            }
        }).start();
    }

    private static String getAppName(File jar) {
        if (jar.getName().endsWith(".jar")) {
            String serviceName;
            serviceName = jar.getName();
            serviceName = serviceName.substring(0, serviceName.lastIndexOf("."));
            return serviceName;
        }
        return jar.getName();
    }



    /**
     * it will block until the service is finished
     *
     */
    protected void stopInternal(boolean forceStop) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        if (appClass != null) {
            Method stop = appClass.getMethod("stop",boolean.class);
            Object invoke = stop.invoke(null, forceStop);
            log.info("process {} has been stopped",appClass.getName());
        } else {
            log.warn("unable to find app with name :{} in app container", this.name());
        }
    }


}
