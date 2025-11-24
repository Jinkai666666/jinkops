package com.jinkops.service;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// 操作日志服务类
@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository){
        this.operationLogRepository = operationLogRepository;
    }

    // 分页获取全部日志（按时间倒序）
    public Page<OperationLogEntity> getLogs(Pageable pageable){

        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        return operationLogRepository.findAll(sorted);
    }

    // 模糊搜索（用户名或操作名称）
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

    // 按时间区间筛选
    public Page<OperationLogEntity> getLogsByTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {

        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        return operationLogRepository.findByTimestampRange(start, end, sorted);
    }

}
