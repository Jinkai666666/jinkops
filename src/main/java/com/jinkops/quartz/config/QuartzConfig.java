package com.jinkops.quartz.config;

import com.jinkops.quartz.job.ScanFailedOperationLogJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    /**
     * JobDetail：定义要跑哪个 Job
     */
    @Bean
    public JobDetail scanFailedOperationLogJobDetail() {
        return JobBuilder.newJob(ScanFailedOperationLogJob.class)
                .withIdentity("scanFailedOperationLogJob")
                .storeDurably()
                .build();
    }

    /**
     * Trigger：定义多久跑一次
     */
    @Bean
    public Trigger scanFailedOperationLogJobTrigger() {

        SimpleScheduleBuilder schedule =
                SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(1)
                        .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(scanFailedOperationLogJobDetail())
                .withIdentity("scanFailedOperationLogTrigger")
                .withSchedule(schedule)
                .build();
    }
}
