package com.jinkops.service;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

// 操作日誌服務類
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    // 分頁獲取全部日誌（按時間倒序）
    public Page<OperationLogEntity> getLogs(Pageable pageable){

        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        return operationLogRepository.findAll(sorted);
    }

    // 模糊搜索（用戶名或操作名稱）
    public Page<OperationLogEntity> searchLogs(String keyword, Pageable pageable) {

        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
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
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        return operationLogRepository.findByCreateTimeRange(start, end, sorted);
    }


    /**
     * ES 掛掉時的 DB 兜底搜尋
     */
    public List<OperationLogEntity> search(String keyword,
                                           Long startTime,
                                           Long endTime) {

        LocalDateTime start = startTime == null
                ? null
                : LocalDateTime.ofEpochSecond(startTime / 1000, 0, ZoneOffset.UTC);

        LocalDateTime end = endTime == null
                ? null
                : LocalDateTime.ofEpochSecond(endTime / 1000, 0, ZoneOffset.UTC);

        // 全空 → 最近 100 筆
        if ((keyword == null || keyword.isBlank()) && start == null && end == null) {
            return operationLogRepository.findTop100ByOrderByCreateTimeDesc();
        }

        return operationLogRepository.searchForEsFallback(keyword, start, end);
    }

}
