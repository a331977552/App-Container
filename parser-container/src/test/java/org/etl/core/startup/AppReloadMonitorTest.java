package org.etl.core.startup;

import org.etl.core.exception.LifecycleException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class AppReloadMonitorTest {

    @Test
    void reset() {

    }

    @Test
    void start() throws LifecycleException, InterruptedException {
        int reloadSpent = 800;
        TestAppWrapper standardAppWrapper = new TestAppWrapper(reloadSpent);
        AppReloadMonitor appReloadMonitor = new AppReloadMonitor(standardAppWrapper,2000);
        int oneFileCheckCycle =  2000 + 10;
        standardAppWrapper.init();
        appReloadMonitor.init();
        standardAppWrapper.start();
        appReloadMonitor.start();// start a thread
        Thread.sleep(10);// ensure the monitor is sleeping
        appReloadMonitor.put(new File(""));
        Thread.sleep(reloadSpent); // mimic that while the monitor is sleeping, the file has been updated twice in 800 milliseconds
        appReloadMonitor.put(new File("123"));
        Thread.sleep(oneFileCheckCycle); // wait for the reloading job done.  left 1200ms to check if file changed, and it takes 800 the reload so in total 2000
        assertEquals(1, standardAppWrapper.reloadTimes);
        Thread.sleep(oneFileCheckCycle); // wait for the cycle to prove there is nothing to be loading
        assertEquals(1, standardAppWrapper.reloadTimes);
        appReloadMonitor.stop();
        appReloadMonitor.start();
        appReloadMonitor.put(new File("tess"));
        Thread.sleep(oneFileCheckCycle+reloadSpent); // wait for the cycle to prove there is nothing to be loading
        assertEquals(2, standardAppWrapper.reloadTimes);

        appReloadMonitor.stop();
        appReloadMonitor.destroy();
        standardAppWrapper.stop();
        standardAppWrapper.destroy();
    }

    @Test
    void stopUntilTerminated() {
    }
}