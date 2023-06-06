package org.etl.core.loader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.etl.core.AppWrapper;
import org.etl.core.FileMonitorService;

import java.io.File;

import static org.etl.core.startup.BootStrap.APP_HOME;

@Slf4j
public class AppLoader implements Loader {
    private static final Object lock = new Object();

    private AppClassLoader appClassLoader;
    private AppWrapper appWrapper;
    private FileAlterationObserver appFileAlterationObserver;

    @Override
    public void setAppWrapper(AppWrapper appWrapper) {
        this.appWrapper = appWrapper;
    }

    public void setAppClassLoader(AppClassLoader appClassLoader) {
        this.appClassLoader = appClassLoader;
    }

    @Override
    public AppWrapper getAppWrapper() {
        return appWrapper;
    }

    @Override
    public void backgroundProcess() {


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
        //todo, 检测该目录App 目录下的所有文件,但凡有一个文件被动过,重启整个APP
        appFileAlterationObserver = new FileAlterationObserver(new File(APP_HOME, getAppWrapper().name()));
        appFileAlterationObserver.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(File file) {
                log.info("file {} modified, restarting app {}", file.getAbsolutePath(), getAppWrapper().name());
                synchronized (lock) {
                    Thread currentThread = Thread.currentThread();
                    ClassLoader originalTccl = currentThread.getContextClassLoader();
                    try {
                        currentThread.setContextClassLoader(AppLoader.class.getClassLoader());
                        getAppWrapper().reload();
                    } finally {
                        currentThread.setContextClassLoader(originalTccl);
                    }
                }
            }
        });
        FileMonitorService.getInstance().addObserver(appFileAlterationObserver);
    }

    @Override
    public ClassLoader getClassLoader() {
        return appClassLoader;
    }



    @Override
    public void stopBackgroundProcess() {
        FileMonitorService.getInstance().removeObserver(appFileAlterationObserver);
    }

    @Override
    public boolean modified() {
        return this.appClassLoader.modified();
    }
}
