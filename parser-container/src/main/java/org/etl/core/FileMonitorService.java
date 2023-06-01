package org.etl.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

@Slf4j
public class FileMonitorService {
    private final File dirToMonitor;
    private FileAlterationListenerAdaptor onFileAlterationListenerAdaptor;

    public FileMonitorService(File dirToMonitor) {
        this.dirToMonitor = dirToMonitor;
    }

    public void setFileAlterationListenerAdaptor(FileAlterationListenerAdaptor onFileAlterationListenerAdaptor) {
        this.onFileAlterationListenerAdaptor = onFileAlterationListenerAdaptor;
    }

    public void start() throws Exception {
        FileAlterationObserver observer = new FileAlterationObserver(dirToMonitor.getAbsolutePath(), pathname -> pathname.getName().endsWith(".jar"));
        observer.addListener(onFileAlterationListenerAdaptor);
        FileAlterationMonitor monitor = new FileAlterationMonitor(3000, observer);
        monitor.start();
    }
}
