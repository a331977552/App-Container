package org.etl.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import java.io.File;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
final class AppDropMonitor {

    private final Map<String, BlockingQueue<File>> appQueue = new ConcurrentHashMap<>();
    private FileMonitor fileMonitor;

    private BiConsumer<String, File> appConsumer;
    private final String appMountPath;
    private Thread appDropNotifierThread;
    private Server server;

    public AppDropMonitor(String appMountPath) {
        this.appMountPath = appMountPath;
    }

    public void setServer(Server server){
        this.server = server;
    }
    public Server getServer(){
        return server;
    }
    public String getAppMountPath() {
        return appMountPath;
    }


    public void startBackgroundProcess() {
        File rootPath = new File(getAppMountPath());
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            List<File> existingApp = findExistingApp(rootPath);
            for (File file : existingApp) {
                addToQueueForDeployment(getAppName(file), file);
            }
        }).start();
        appDropNotifierThread = new Thread(() -> {
            while (server.isAvailable()) {
                //check all
                for (BlockingQueue<File> next : appQueue.values()) {
                    File poll = next.poll();
                    if (poll != null) {
                        log.info("deploying new app: {}", getAppName(poll));
                        if (appConsumer == null) {
                            log.error("app drop lisenter not set!!!!");
                            return;
                        }
                        if (!Thread.interrupted()) {
                            appConsumer.accept(getAppName(poll), poll);
                        } else {
                            return ;
                        }
                    }
                }
                try {
                    // the app can be modified, or redeployed multiple times in  3 s.
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
            appQueue.clear();
            log.info("cleaned queued apps waiting for deployment");
        });
        appDropNotifierThread.start();

        fileMonitor = new FileMonitor(rootPath, pathname -> pathname.getName().endsWith(".jar"),2000);
        fileMonitor.setFileAlterationListenerAdaptor(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                addToQueueForDeployment(getAppName(file), file);
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
            fileMonitor.start();
        } catch (Exception e) {
            log.error("failed to start app root path monitor", e);
        }
    }

    private List<File> findExistingApp(File rootPath) {
        File[] listFolders = rootPath.listFiles((dir, name) -> dir.equals(rootPath) && new File(dir, name).isDirectory());
        if (listFolders == null) {
            log.info("no app found in app mount path {}", getAppMountPath());
            listFolders = new File[0];
        }
        Map<String, File> appFolderMap = new HashMap<>();
        for (File folder : listFolders) {
            appFolderMap.put(folder.getName(), folder);
        }

        File[] jarList = rootPath.listFiles((dir, name) -> dir.equals(rootPath) && name.endsWith(".jar"));
        if (jarList == null) {
            log.error("app mount directory {} is invalid, please check", rootPath.getAbsolutePath());
            jarList = new File[0];
        }

        for (File jar : jarList) {
            String appName = getAppName(jar);
            if (appFolderMap.containsKey(appName)) {
                appFolderMap.remove(appName);
                log.info("removing {} app folder, as new app jar is taking place", appName);
            }
        }
        Collection<File> values = appFolderMap.values();
        List<File> arrayList = new ArrayList<>(jarList.length + values.size());
        Collections.addAll(arrayList, jarList);
        arrayList.addAll(values);
        return arrayList;
    }

    public void stopBackgroundProcess() {
        if (appDropNotifierThread != null) {
            appDropNotifierThread.interrupt();
        }
        if (fileMonitor != null) {
            fileMonitor.stop();
        }
    }


    /**
     * @param appName
     * @param appFile it can be a jar file or directory
     */
    private void addToQueueForDeployment(String appName, File appFile) {
        BlockingQueue<File> files = appQueue.get(appName);
        if (files == null) {
            files = new ArrayBlockingQueue<>(1);
            appQueue.put(appName, files);
        }
        try {
            files.clear();
            files.put(appFile);
            log.info("new app {} is waiting for redeployment", appName);
        } catch (InterruptedException ignored) {

        }
    }

    public void setOnAppDroppedListener(BiConsumer<String, File> appConsumer) {
        this.appConsumer = appConsumer;
    }

    private static String getAppName(File jar) {
        if (!jar.isDirectory() && jar.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
            String serviceName;
            serviceName = jar.getName();
            serviceName = serviceName.substring(0, serviceName.indexOf(".jar"));
            return serviceName;
        }
        return jar.getName();
    }
}
