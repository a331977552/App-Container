package org.etl.core.loader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.AppWrapper;
import org.etl.core.FileMonitorService;

import java.io.File;

import static org.etl.core.startup.BootStrap.APP_HOME;

@Slf4j
public class AppLoader implements Loader{
    private static final Object lock = new Object();

    private AppClassLoader appClassLoader;
    private AppWrapper context;

    @Override
    public void setContext(AppWrapper context)
    {
        this.context = context;
    }

    public void setAppClassLoader(AppClassLoader appClassLoader){
        this.appClassLoader = appClassLoader;
    }
    @Override
    public AppWrapper getContext() {
        return context;
    }

    @Override
    public void backgroundProcess() {


        AppWrapper context = getContext();
        if (context != null) {
            if (context.getReloadable() && modified()) {
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
                FileMonitorService fileMonitorService = new FileMonitorService(new File(APP_HOME,getContext().name()));
                fileMonitorService.setFileAlterationListenerAdaptor(new FileAlterationListenerAdaptor() {
                    @Override
                    public void onFileChange(File file) {
                        //todo, 检测该目录App 目录下的所有文件,但凡有一个文件被动过,重启整个APP
                        log.info("file {} modified, restarting app {}",file.getAbsolutePath(),getContext().name());
                        synchronized (lock) {
                            Thread currentThread = Thread.currentThread();
                            ClassLoader originalTccl = currentThread.getContextClassLoader();
                            try {
                                currentThread.setContextClassLoader(AppLoader.class.getClassLoader());
                                context.reload();
                            } finally {
                                currentThread.setContextClassLoader(originalTccl);
                            }
                        }
                    }
                });
                try {
                    fileMonitorService.start();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return appClassLoader;
    }

    @Override
    public boolean modified() {
        return appClassLoader != null && appClassLoader.modified();
    }
}
