package com.jinkops.quartz.config;

import com.jinkops.quartz.job.DemoQuartzJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail demoJobDetail() {
        return JobBuilder.newJob(DemoQuartzJob.class)
                .withIdentity("demoJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger demoJobTrigger() {
        SimpleScheduleBuilder schedule =
                SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(5)
                        .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(demoJobDetail())
                .withIdentity("demoTrigger")
                .withSchedule(schedule)
                .build();
    }
}
