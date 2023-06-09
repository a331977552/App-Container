package org.etl.core;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.loader.AppClassLoader;
import org.etl.core.loader.AppLoader;
import org.etl.core.startup.JarDeployer;
import org.etl.core.startup.StandardAppWrapper;
import org.etl.service.Context;
import org.etl.service.StandardContext;
import org.etl.service.service.CacheUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static org.etl.core.Constants.APP_CONFIG_FILE_NAME;

@Slf4j
@Component
public class StandardServer implements Server, BiConsumer<String, File> {


    private final Map<String, AppWrapper> apps =new ConcurrentHashMap<>();
    private String appMountPath;
    private Context context;

    private final AppDropMonitor appDropMonitor = new AppDropMonitor();

    private boolean started = false;
    protected void addApp(AppWrapper app) {
        try {
            app.start();
        } catch (Exception e) {
            log.error("failed to start app: "+app.getName(),e);
        }
        apps.put(app.getName(),app);
    }

    @Override
    public void start() {
        //block util context is generated.
        prepareContext();
        appDropMonitor.setOnAppDropped(this);
        appDropMonitor.setAppMountPath(getAppMountPath());
        this.backgroundProcess();
        started = true;

    }

    private void prepareContext() {
        //todo prepare
        StandardContext context = new StandardContext();
        context.setArgs(new String[]{"TEST"});
        context.setCacheUtil(new CacheUtil());
        this.context = context;
    }

    @Override
    public void stop() {
        //todo
        stopBackgroundProcess();
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
        started = false;
    }

    private void stopBackgroundProcess() {
        appDropMonitor.stopBackgroundProcess();
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
        appDropMonitor.startBackgroundProcess();

    }


    private void deployNewApp(String name,File file) {
        AppWrapper app = getApp(name);
        if (app!=null){
            removeApp(name);
        }
        String appPath = getAppMountPath() + File.separator + name;
        if (file.getName().endsWith(".jar")){
            JarDeployer instance = JarDeployer.getInstance();
            instance.deleteExistingExplodedApp(appPath);
            instance.unzip(file,appPath);
            instance.deleteJar(file);
        }else if(!file.isDirectory()){//might be an already exploded app
            log.warn("unrecognized app type {}",file.getAbsolutePath());
            return ;
        }
        File appDirFile = new File(appPath);
//        G:\java_workspace\parser-parent\apps\parser-app-3.1
//        G:\java_workspace\parser-parent\apps\parser-app-3.1.0
        if (!validateAppDir(appDirFile))
        {
            return;
        }

        StandardAppWrapper standardAppWrapper = new StandardAppWrapper(appPath);
        standardAppWrapper.setAppMountPath(getAppMountPath());
        standardAppWrapper.setName(name);
        AppLoader appLoader = new AppLoader();
        appLoader.setAppWrapper(standardAppWrapper);
        appLoader.setAppClassLoader(new AppClassLoader(getAppMountPath(),name));
        standardAppWrapper.setLoader(appLoader);
        standardAppWrapper.setContext(getContext());
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

    @Override
    public Context getContext() {
        return this.context;
    }

    @Nullable
    @Override
    public AppWrapper getApp(String name) {
        return apps.get(name);
    }



    public final boolean validateAppDir(File appDir){
        File[] files = appDir.listFiles();
        if (files==null){
            log.error("invalid app directory structure {},there are no files",appDir.getAbsolutePath());
            return false;
        }
        File appBootInfo = new File(appDir, "BOOT-INF");
        Set<File> set = new HashSet<>(Arrays.asList(files));

        if (!set.contains(appBootInfo))
        {
            log.error("invalid app directory structure {},unable to find dir: {}",appDir.getAbsolutePath(),appBootInfo);
            return false;
        }

        File classes = new File(appBootInfo, "classes");
        File[] classesDir = classes.listFiles();
        if (classesDir==null){
            log.error("invalid app directory structure {},there are no files under classes folder",appDir.getAbsolutePath());
            return false;
        }

        File appConfigFile = new File(classes, APP_CONFIG_FILE_NAME);

        File lib = new File(appBootInfo, "lib");

        if (!appConfigFile.exists()){
            log.error("config file doesn't exist: {}",appDir.getAbsolutePath());
            return false;
        }
        if (!lib.exists() || !lib.isDirectory()){
            log.error("lib folder doesn't exist: {}",appDir.getAbsolutePath());
            return false;
        }

        return true;
    }


    @Override
    public void accept(String appName, File file) {
        this.deployNewApp(appName,file);
    }
}
