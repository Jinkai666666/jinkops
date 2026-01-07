package com.jinkops.quartz.job;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;
import com.jinkops.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quartz Job:
 * - 周期扫描失败的操作日志
 * - 当前阶段只做日志验证，不做任何补偿写
 */
@Slf4j
@Configuration
@RequiredArgsConstructor

public class ScanFailedOperationLogJob implements Job {
    @Autowired
    private OperationLogRepository operationLogRepository;

    @Override
    public void execute(JobExecutionContext context) {
        // 5 分钟前
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        //只讀掃描
        List<OperationLogEntity>failedLogs=
                operationLogRepository.findBefore(threshold);
        log.info(
                "[Quartz] scan operation logs, threshold={}, count={}",
                threshold,
                failedLogs.size()
        );
    }
}
