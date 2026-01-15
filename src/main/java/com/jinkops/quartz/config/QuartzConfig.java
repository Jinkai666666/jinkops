package com.jinkops.quartz.config;

import com.jinkops.quartz.job.ScanFailedOperationLogJob;
import org.quartz.JobDetail;
import org.quartz.JobBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    /**
     * JobDetail：定義要跑哪個 Job
     */
    @Bean
    public JobDetail scanFailedOperationLogJobDetail() {
        return JobBuilder.newJob(ScanFailedOperationLogJob.class)
                .withIdentity("scanFailedOperationLogJob")
                .storeDurably()
                .build();
    }

    /**
     * Trigger：定義多久跑一次
     */
    @Bean
    public Trigger scanFailedOperationLogJobTrigger() {

        SimpleScheduleBuilder schedule =
                SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(15)
                        .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(scanFailedOperationLogJobDetail())
                .withIdentity("scanFailedOperationLogTrigger")
                .withSchedule(schedule)
                .build();
    }
}
