package org.etl.schedule;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class MyScheduler {

    public void schedule() {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.start();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("parsing", "group1")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(30)
                            .repeatForever())
                    .build();

            JobDetail job = JobBuilder.newJob(ParsingJob.class)
                    .withIdentity("parsing", "group1")
                    .build();
            System.out.println("scheduleJob : "+ job);
            scheduler.scheduleJob(job, trigger);

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
