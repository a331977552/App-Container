package org.etl.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.*;

public class RandomTest {

    @Test
    public void test() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        ArrayList<Future<?>> objects = new ArrayList<>(10);

        for (int i = 0; i < 10; i++) {
            objects.add(executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        int min = Math.max(1000, new Random().nextInt(10000));

                        Thread.sleep(min);
                        System.out.println(min);
                        System.out.println("finished "+this);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        }
        boolean allDone = false;
        int doneTasks = 0;
        while (!allDone) {
            Iterator<Future<?>> iterator = objects.iterator();
            while (iterator.hasNext()) {
                Future<?> next = iterator.next();
                boolean done = next.isDone();
                if (done) {
                    iterator.remove();
                    doneTasks++;
                    System.out.println("task is done: " + next);
                }
            }
            if (doneTasks == 10)
                allDone = true;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
