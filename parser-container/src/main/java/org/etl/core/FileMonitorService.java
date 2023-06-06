package org.etl.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class FileMonitorService {
    private final File dirToMonitor;
    private final FileFilter fileFilter;
    private FileAlterationListenerAdaptor onFileAlterationListenerAdaptor;
    private FileAlterationMonitor monitor;

    private static AtomicInteger threadCount = new AtomicInteger(0);

    public FileMonitorService(File dirToMonitor, FileFilter fileFilter) {
        this.dirToMonitor = dirToMonitor;
        this.fileFilter = fileFilter;
    }

    public void setFileAlterationListenerAdaptor(FileAlterationListenerAdaptor onFileAlterationListenerAdaptor) {
        this.onFileAlterationListenerAdaptor = onFileAlterationListenerAdaptor;
    }

    public void start() throws Exception {
        FileAlterationObserver observer = new FileAlterationObserver(dirToMonitor.getAbsolutePath(), fileFilter);
        observer.addListener(onFileAlterationListenerAdaptor);
        monitor = new FileAlterationMonitor(3000, observer);
        monitor.setThreadFactory(r -> new Thread(r, "FileMonitor-Thread-" + threadCount.getAndIncrement()));
        monitor.start();
    }

    public void stop(){
        try {
            monitor.stop();
        } catch (Exception e) {
            log.error("unable to stop file monitor!!!", e);
        }
    }


}
