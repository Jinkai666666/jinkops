package com.jinkops.quartz.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Demo Quartz Job
 * 每次执行只打印一行日志
 */
@Slf4j
public class DemoQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("[Quartz] DemoQuartzJob executed");
    }
}
