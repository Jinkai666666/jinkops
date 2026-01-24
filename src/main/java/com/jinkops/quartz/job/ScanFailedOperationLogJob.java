package com.jinkops.quartz.job;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 只做兜底掃描，不改業務資料
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScanFailedOperationLogJob implements Job {
    private final OperationLogRepository operationLogRepository;

    @Override
    public void execute(JobExecutionContext context) {
        // 只掃描過期的失敗日誌
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        // 只讀掃描，不做補償
        List<OperationLogEntity> failedLogs =
                operationLogRepository.findBefore(threshold);
        log.info(
                "[Quartz] scan operation logs, threshold={}, count={}",
                threshold,
                failedLogs.size()
        );
    }
}
