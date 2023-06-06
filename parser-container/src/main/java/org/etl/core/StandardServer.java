package org.etl.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.etl.core.startup.StandardAppWrapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileFilter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.etl.core.startup.BootStrap.APP_HOME;

@Slf4j
@Component
public class StandardServer implements Server {

    private final Map<String, AppWrapper> apps = new ConcurrentHashMap<>();

    @Override
    public void addApp(AppWrapper app) {
        apps.put(app.name(), app);
    }

    @Override
    public void start() {
        FileMonitorService.getInstance().start();
        for (Map.Entry<String, AppWrapper> appWrapperEntry : apps.entrySet()) {
            try {
                appWrapperEntry.getValue().start();
            } catch (Exception e) {
                log.error("failed to start app: " + appWrapperEntry.getValue().name(), e);
            }
        }
        this.backgroundProcess();
    }

    @Override
    public void stop() {
        for (Map.Entry<String, AppWrapper> appWrapperEntry : apps.entrySet()) {
            try {
                appWrapperEntry.getValue().stop();
            } catch (Exception e) {
                log.error("failed to stop app: " + appWrapperEntry.getValue().name(), e);
            }
        }
        FileMonitorService.getInstance().stop();
    }

    protected void backgroundProcess() {
        FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(new File(APP_HOME), pathname -> pathname.getName().endsWith(".jar"));
        fileAlterationObserver.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                deployNewApp(file);
            }
        });
        FileMonitorService.getInstance().addObserver(fileAlterationObserver);
    }

    private void deployNewApp(File file){
        try {
//                    check if the same name is already exist
            String name = file.getName();
            AppWrapper app = getApp(name);
            if (app != null) {
                removeApp(file.getName());
            }
            StandardAppWrapper standardAppWrapper = new StandardAppWrapper();
            standardAppWrapper.setName(file.getName());
            standardAppWrapper.setJarDeployer();
            StandardServer.this.addApp(standardAppWrapper);
            standardAppWrapper.start();

        } catch (Exception e) {
            log.error("failed to deploy new app: {}, please check", file.getName(), e);
        }
    }
    public AppWrapper getApp(String appName) {
        return apps.get(appName);
    }

    @Override
    public AppWrapper removeApp(String appName) {
        AppWrapper remove = apps.remove(appName);
        if (remove != null) {
            remove.stop();
        } else {
            log.warn("app {} doesn't exist", appName);
        }
        return remove;
    }
}
