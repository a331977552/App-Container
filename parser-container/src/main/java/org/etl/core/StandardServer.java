package org.etl.core;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.loader.AppClassLoader;
import org.etl.core.loader.AppLoader;
import org.etl.core.startup.JarDeployer;
import org.etl.core.startup.StandardAppWrapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StandardServer implements Server {

    private final Map<String, AppWrapper> apps =new ConcurrentHashMap<>();
    private String appMountPath;
    private FileMonitorService fileMonitorService;

    @Override
    public void addApp(AppWrapper app) {
        try {
            app.start();
        } catch (Exception e) {
            log.error("failed to start app: "+app.getName(),e);
        }
        apps.put(app.getName(),app);
    }

    @Override
    public void start() {
        //todo
        for (Map.Entry<String, AppWrapper> appWrapperEntry : apps.entrySet()) {
            AppWrapper value = appWrapperEntry.getValue();
            if (!value.started()){
                try {
                    value.start();
                } catch (Exception e) {
                    log.error("failed to start app : {}",appWrapperEntry.getValue().getName(),e);
                }
            }
        }
        this.backgroundProcess();

    }

    @Override
    public void stop() {
        //todo
        fileMonitorService.stop();
        for (Map.Entry<String, AppWrapper> appWrapperEntry : apps.entrySet()) {
            AppWrapper value = appWrapperEntry.getValue();
            if (value.started()){
                try {
                    value.stop();
                } catch (Exception e) {
                    log.error("failed to stop app : {}",appWrapperEntry.getValue().getName(),e);
                }
            }
        }
    }

    @Override
    public void setAppMountPath(String appMountPath) {
        this.appMountPath = appMountPath;
    }

    @Override
    public String getAppMountPath() {
        return appMountPath;
    }

    protected void backgroundProcess(){
        //todo periodically check if a new folder or jar file dropped, and deploy it
        File rootPath = new File(getAppMountPath());
        new Thread(() -> {
            String[] jarList = rootPath.list((dir, name) -> name.endsWith(".jar"));
            if (jarList == null)
            {
                log.error("app mount directory {} is invalid, please check",rootPath.getAbsolutePath());
                return ;
            }
            for (String jar : jarList) {
                File jarFile = new File(rootPath,jar);
                deployNewApp(jarFile);
            }
        }).start();
        fileMonitorService = new FileMonitorService(rootPath, pathname -> pathname.getName().endsWith(".jar"));
        fileMonitorService.setFileAlterationListenerAdaptor(new FileAlterationListenerAdaptor(){
            @Override
            public void onFileCreate(File file) {
                deployNewApp(file);
            }

            @Override
            public void onFileChange(File file) {
                log.info("todo, onFileChange");
            }

            @Override
            public void onFileDelete(File file) {
                log.info("todo, onfile onFileDelete");

            }
        });
        try {
            fileMonitorService.start();
        } catch (Exception e) {
            log.error("failed to start app root path monitor",e);
        }
    }

    private void deployNewApp(File zippedJar) {
        String name = getAppName(zippedJar);
        AppWrapper app = getApp(name);
        if (app!=null){
            removeApp(name);
        }
        String appPath = getAppMountPath() + File.separator + name;
        JarDeployer instance = JarDeployer.getInstance();
        instance.deleteExistingExplodedApp(appPath);
        instance.unzip(zippedJar,appPath);
        instance.deleteJar(zippedJar);
        StandardAppWrapper standardAppWrapper = new StandardAppWrapper(appPath);
        standardAppWrapper.setAppMountPath(getAppMountPath());
        standardAppWrapper.setName(name);

        AppLoader appLoader = new AppLoader();
        appLoader.setAppWrapper(standardAppWrapper);
        appLoader.setAppClassLoader(new AppClassLoader(getAppMountPath(),name));

        standardAppWrapper.setLoader(appLoader);

        standardAppWrapper.setReloadable(true);
        StandardServer.this.addApp(standardAppWrapper);
    }

    @Override
    public AppWrapper removeApp(String appName) {
        AppWrapper remove = apps.remove(appName);
        if (remove!=null){
            remove.stop();
        }else{
            log.warn("app {} doesn't exist",appName);
        }
        return remove;
    }

    @Nullable
    @Override
    public AppWrapper getApp(String name) {
        return apps.get(name);
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
}
