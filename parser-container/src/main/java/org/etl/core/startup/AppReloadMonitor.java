package org.etl.core.startup;

import lombok.extern.slf4j.Slf4j;
import org.etl.core.BaseLifeCycle;
import org.etl.core.Reloadable;
import org.etl.core.exception.LifecycleException;

import java.io.File;
import java.util.concurrent.*;

/***
 * the app reload monitor will be stopped every time a reload action is fired to stop multip reload at same time.
 */
@Slf4j
public final class AppReloadMonitor extends BaseLifeCycle implements Runnable {
    private final Reloadable reloadable;
    private final long intervalInMillisecond;
    private final BlockingQueue<File> appFile = new ArrayBlockingQueue<>(1);
    private final CyclicBarrier stopper = new CyclicBarrier(2);
    private ExecutorService executorService;

    public AppReloadMonitor(Reloadable reloadable, long intervalInMillisecond) {
        this.reloadable = reloadable;
        this.intervalInMillisecond = intervalInMillisecond;
    }

    public void put(File file) {
        try {
            appFile.clear();
            appFile.put(file);
            log.info("file updated: " + appFile);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void run() {
        try {
            while (isAvailable()) {
                System.out.println("check alll ");
                //check all
                File poll = appFile.poll();
                if (poll != null) {
                    log.info("app {} has changed", poll);
                    executorService.execute(reloadable::reload);
                    break;//once there is reload request, the app reload will stop monitor and wait for the app wrapper to stop this monitor
                }
                try {
                    Thread.sleep(intervalInMillisecond);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        } finally {
            try {
                stopper.await();
            } catch (InterruptedException | BrokenBarrierException ignored) {
            }
        }
    }


    @Override
    protected void internalInit() throws LifecycleException {
        executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    protected void internalStart() throws LifecycleException {
        log.info("start app reload monitor {}", this);
        stopper.reset();
        executorService.execute(this);
    }

    @Override
    protected void internalStop() throws LifecycleException {
        appFile.clear();
        try {
            stopper.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            log.error("unexpected behavior ,please check", e);
        }
        log.info("app reload monitor {} stopped", this);
    }

    @Override
    protected void internalDestroy() throws LifecycleException {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1800, TimeUnit.SECONDS)) {
                log.warn("unable to terminate thread pool, please check !!");
            }
        } catch (InterruptedException e) {
            log.error("unexpected behavior, please check", e);
        }
    }
}
