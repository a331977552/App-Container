package org.etl.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class FileMonitorService {
    private final File dirToMonitor;
    private FileAlterationListenerAdaptor onFileAlterationListenerAdaptor;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public FileMonitorService(File dirToMonitor) {
        this.dirToMonitor = dirToMonitor;
    }

    public void setFileAlterationListenerAdaptor(FileAlterationListenerAdaptor onFileAlterationListenerAdaptor) {
        this.onFileAlterationListenerAdaptor = onFileAlterationListenerAdaptor;
    }
    public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler){
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }
    public void start() throws Exception {
        FileAlterationObserver observer = new FileAlterationObserver(dirToMonitor.getAbsolutePath(), pathname -> pathname.getName().endsWith(".jar"));
        observer.addListener(onFileAlterationListenerAdaptor);
        FileAlterationMonitor monitor = new FileAlterationMonitor(3000, observer);
        monitor.setThreadFactory(r -> {
            Thread thread = new Thread(r,"FileMonitor-Thread");
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            return thread;
        });
        monitor.start();
    }


}
