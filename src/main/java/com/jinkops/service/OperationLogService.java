package com.jinkops.service;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// 操作日誌服務類
@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository){
        this.operationLogRepository = operationLogRepository;
    }

    // 分頁獲取全部日誌（按時間倒序）
    public Page<OperationLogEntity> getLogs(Pageable pageable){

        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        return operationLogRepository.findAll(sorted);
    }

    // 模糊搜索（用戶名或操作名稱）
    public Page<OperationLogEntity> searchLogs(String keyword, Pageable pageable) {

        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        if (keyword == null || keyword.trim().isEmpty()) {
            return operationLogRepository.findAll(sorted);
        }

        return operationLogRepository.searchLogs(keyword, sorted);
    }

    // 按時間區間篩選
    public Page<OperationLogEntity> getLogsByTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {

        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        return operationLogRepository.findByTimestampRange(start, end, sorted);
    }

}
