package com.jinkops.service;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;
import com.jinkops.service.es.OperationLogEsService;
import com.jinkops.vo.LogQueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

// 日誌查詢服務，專門處理讀場景
@Service
@Slf4j
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final OperationLogEsService operationLogEsService;

    // 日誌列表查詢，按時間倒序
    public Page<OperationLogEntity> getLogs(Pageable pageable) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] getLogs start keyParams=page={},size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        try {
            PageRequest sorted = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createTime")
            );

            Page<OperationLogEntity> result = operationLogRepository.findAll(sorted);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] getLogs success cost={}ms keyResult=total={}",
                    cost, result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] getLogs failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 關鍵字模糊查詢
    public Page<OperationLogEntity> searchLogs(String keyword, Pageable pageable) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] searchLogs start keyParams=keyword={},page={},size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());
        try {
            PageRequest sorted = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createTime")
            );

            Page<OperationLogEntity> result;
            if (keyword == null || keyword.trim().isEmpty()) {
                result = operationLogRepository.findAll(sorted);
            } else {
                result = operationLogRepository.searchLogs(keyword, sorted);
            }
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] searchLogs success cost={}ms keyResult=total={}",
                    cost, result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] searchLogs failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 時間區間查詢
    public Page<OperationLogEntity> getLogsByTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        long begin = System.currentTimeMillis();
        log.info("[SERVICE] getLogsByTimeRange start keyParams=start={},end={},page={},size={}",
                start, end, pageable.getPageNumber(), pageable.getPageSize());
        try {
            PageRequest sorted = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createTime")
            );

            Page<OperationLogEntity> result =
                    operationLogRepository.findByCreateTimeRange(start, end, sorted);
            long cost = System.currentTimeMillis() - begin;
            log.info("[SERVICE] getLogsByTimeRange success cost={}ms keyResult=total={}",
                    cost, result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] getLogsByTimeRange failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 統一查詢入口，避免 Controller 自己分流
    public Page<OperationLogEntity> page(LogQueryRequest req) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] page start keyParams=keyword={},start={},end={},page={},size={}",
                req.getKeyword(), req.getStartTime(), req.getEndTime(), req.getPage(), req.getSize());
        try {
            Pageable pageable = PageRequest.of(
                    req.getPage(),
                    req.getSize(),
                    Sort.by(Sort.Direction.DESC, "createTime")
            );

            Page<OperationLogEntity> result;
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                result = searchLogs(req.getKeyword(), pageable);
            } else if (req.getStartTime() != null
                    && !req.getStartTime().isBlank()
                    && req.getEndTime() != null
                    && !req.getEndTime().isBlank()) {
                result = getLogsByTimeRange(
                        LocalDateTime.parse(req.getStartTime()),
                        LocalDateTime.parse(req.getEndTime()),
                        pageable
                );
            } else {
                result = getLogs(pageable);
            }
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] page success cost={}ms keyResult=total={}",
                    cost, result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] page failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // ES 掛掉時直接走 DB 兜底
    public List<OperationLogEntity> search(String keyword,
                                           Long startTime,
                                           Long endTime) {
        // DB 兜底入口，給 ES 掛掉或搜尋空結果時用
        long begin = System.currentTimeMillis();
        log.info("[SERVICE] searchFallback start keyParams=keyword={},start={},end={}",
                keyword, startTime, endTime);

        LocalDateTime start = startTime == null
                ? null
                : LocalDateTime.ofEpochSecond(startTime / 1000, 0, ZoneOffset.UTC);

        LocalDateTime end = endTime == null
                ? null
                : LocalDateTime.ofEpochSecond(endTime / 1000, 0, ZoneOffset.UTC);

        try {
            List<OperationLogEntity> result;
            if ((keyword == null || keyword.isBlank()) && start == null && end == null) {
                // 全空條件直接回最近 100 筆
                result = operationLogRepository.findTop100ByOrderByCreateTimeDesc();
            } else {
                result = operationLogRepository.searchForEsFallback(keyword, start, end);
            }
            long cost = System.currentTimeMillis() - begin;
            log.info("[SERVICE] searchFallback success cost={}ms keyResult=count={}",
                    cost, result.size());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] searchFallback failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // ES 只是搜尋副本，異常時直接回 DB
    public List<OperationLogEntity> searchEs(String keyword, Long startTime, Long endTime) {
        // 先查 ES，加快搜尋；失敗就回 searchFallback
        long start = System.currentTimeMillis();
        log.info("[SERVICE] searchEs start keyParams=keyword={},start={},end={}",
                keyword, startTime, endTime);
        try {
            List<OperationLogEntity> esResult =
                    operationLogEsService.search(keyword, startTime, endTime);

            List<OperationLogEntity> result;
            String source;
            if (esResult == null || esResult.isEmpty()) {
                result = search(keyword, startTime, endTime);
                source = "DB";
            } else {
                result = esResult;
                source = "ES";
            }

            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] searchEs success cost={}ms keyResult=source={},count={}",
                    cost, source, result.size());
            return result;

        } catch (Exception e) {
            log.error("[SERVICE] searchEs failed reason={}", e.getMessage(), e);
            List<OperationLogEntity> result = search(keyword, startTime, endTime);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] searchEs success cost={}ms keyResult=source={},count={}",
                    cost, "DB", result.size());
            return result;
        }
    }
}
