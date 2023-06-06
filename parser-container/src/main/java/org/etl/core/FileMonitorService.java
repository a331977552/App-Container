package org.etl.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class FileMonitorService {
    private AtomicInteger threadCount = new AtomicInteger(0);

    private FileAlterationListenerAdaptor onFileAlterationListenerAdaptor;
    private FileAlterationMonitor monitor;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private static final FileMonitorService INSTANCE = new FileMonitorService();
    private FileMonitorService() {
        monitor = new FileAlterationMonitor(3000);
        monitor.setThreadFactory(r -> new Thread(r,"FileMonitor-Thread-"+threadCount.getAndIncrement()));
    }

    public static FileMonitorService getInstance(){
        return INSTANCE;
    }
    public void addObserver(FileAlterationObserver observer){
        monitor.addObserver(observer);
    }
    public void removeObserver(FileAlterationObserver observer){
        monitor.removeObserver(observer);
    }

    public void setFileAlterationListenerAdaptor(FileAlterationListenerAdaptor onFileAlterationListenerAdaptor) {
        this.onFileAlterationListenerAdaptor = onFileAlterationListenerAdaptor;
    }

    public void start() {
        try {
            monitor.start();
            started.set(true);
        } catch (Exception e) {
            log.error("failed to start file monitor, please check ", e);
        }
    }
    public void stop(){
        try {
            monitor.stop();
            started.set(false);
        } catch (Exception e) {
            log.error("failed to stop file monitor service!",e);
        }
    }


}
