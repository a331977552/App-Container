package org.etl.core;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.etl.core.startup.StandardAppWrapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StandardServer implements Server {

    private final Map<String, AppWrapper> apps =new ConcurrentHashMap<>();
    @Override
    public void addApp(AppWrapper app) {
        try {
            app.start();
        } catch (Exception e) {
            log.error("failed to start app: "+app.name(),e);
        }
        apps.put(app.name(),app);
    }

    @Override
    public void start() {
        //todo
    }

    @Override
    public void stop() {
        //todo
    }

    protected void backgroundProcess(){
        //todo periodically check if a new folder or jar file dropped, and deploy it
//        File rootPath = new File(APP_HOME).getCanonicalFile();
//        File[] files = rootPath.listFiles();
//        Objects.requireNonNull(files);
//        Optional<File> first = Arrays.stream(files).filter(f -> f.getName().endsWith(".jar")).findFirst();
//        Optional<File> existProject = Arrays.stream(files).filter(File::isDirectory).findFirst();
//        if (first.isPresent() || existProject.isPresent()) {
//            File file = first.orElseGet(existProject::get);
//            StandardAppWrapper standardAppWrapper = new StandardAppWrapper();
//            standardAppWrapper.setName(file.getName());
//            standardAppWrapper.setJarDeployer();
//            server.addApp(standardAppWrapper);
//        } else {
//            log.warn("unable to find app on root path:{}, please put app", rootPath.getAbsolutePath());
//        }
    }

    @Override
    public AppWrapper removeApp(String appName) {
        AppWrapper remove = apps.remove(appName);
        if (remove!=null){
            remove.stop();
        }else{
            log.warn("app {} doesn't exist",appName);
        }
        return null;
    }
}
