package com.jinkops.service;

import com.jinkops.audit.AuditContext;
import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.mq.producer.EventLogService;
import com.jinkops.repository.OperationLogRepository;
import com.jinkops.service.es.OperationLogEsService;
import com.jinkops.vo.LogQueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final OperationLogEsService operationLogEsService;
    private final EventLogService eventLogService;

    @Qualifier("mqTaskExecutor")
    private final Executor mqTaskExecutor;

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

            // 普通列表本來就是查 DB，前端來源欄直接看這個值就好。
            Page<OperationLogEntity> result = operationLogRepository.findAll(sorted);
            // 關鍵字查詢這條線也不繞 ES，避免前端誤判來源。
            markSource(result, "DB");
            AuditContext.put("logSource", "DB");
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] getLogs success cost={}ms keyResult=total={}",
                    cost, result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] getLogs failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

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

            markSource(result, "DB");
            AuditContext.put("logSource", "DB");
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] searchLogs success cost={}ms keyResult=total={}",
                    cost, result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] searchLogs failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

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

            // 時間區間查詢走 DB，穩定優先。
            Page<OperationLogEntity> result =
                    operationLogRepository.findByCreateTimeRange(start, end, sorted);
            markSource(result, "DB");
            AuditContext.put("logSource", "DB");
            long cost = System.currentTimeMillis() - begin;
            log.info("[SERVICE] getLogsByTimeRange success cost={}ms keyResult=total={}",
                    cost, result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] getLogsByTimeRange failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

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

    private LocalDateTime toLocalDateTime(Long epochMilli) {
        return epochMilli == null
                ? null
                : LocalDateTime.ofEpochSecond(epochMilli / 1000, 0, ZoneOffset.UTC);
    }

    public Page<OperationLogEntity> searchEs(String keyword, Long startTime, Long endTime, int page, int size) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] searchEs start keyParams=keyword={},start={},end={},page={},size={}",
                keyword, startTime, endTime, page, size);
        boolean esAvailable = true;
        try {
            Page<OperationLogEntity> esResult = operationLogEsService.search(keyword, startTime, endTime, page, size);
            if (esResult != null && !esResult.isEmpty()) {
                // ES 活著但 MQ 掛過時，ES 可能是舊資料；這時回源 DB，並只補當頁缺失資料到 MQ。
                Page<OperationLogEntity> dbCheck = searchFallbackPage(keyword, startTime, endTime, page, size);
                if (shouldUseDbFallback(esResult, dbCheck)) {
                    markStaleFallbackSource(dbCheck);
                    AuditContext.put("logSearchSource", "ES_STALE");
                    AuditContext.put("logSearchFallback", "DB");
                    AuditContext.put("logSource", "ES->DB");
                    long cost = System.currentTimeMillis() - start;
                    log.info("[SERVICE] searchEs stale fallback success cost={}ms keyResult=source=DB,total={}",
                            cost, dbCheck.getTotalElements());
                    return dbCheck;
                }

                // ES 查得到且資料沒落後，就直接回 ES。
                markSource(esResult, "ES");
                AuditContext.put("logSearchSource", "ES");
                AuditContext.put("logSearchFallback", "NO");
                AuditContext.put("logSource", "ES");
                long cost = System.currentTimeMillis() - start;
                log.info("[SERVICE] searchEs success cost={}ms keyResult=source=ES,total={}",
                        cost, esResult.getTotalElements());
                return esResult;
            }

            AuditContext.put("logSearchSource", "ES_EMPTY");
        } catch (Exception e) {
            esAvailable = false;
            AuditContext.put("logSearchSource", "ES_ERROR");
            log.error("[SERVICE] searchEs failed on ES query reason={}", e.getMessage(), e);
        }

        // ES 掛掉只回 DB；ES 能連但查不到，才把當頁缺失資料補給 MQ。
        Page<OperationLogEntity> fallback = searchFallbackPage(keyword, startTime, endTime, page, size);
        markSource(fallback, "ES->DB");
        if (esAvailable) {
            sendMissingLogsToMq(fallback.getContent());
        }
        AuditContext.put("logSearchFallback", "DB");
        AuditContext.put("logSource", "ES->DB");
        AuditContext.put("logSearchDbTotal", fallback.getTotalElements());
        long cost = System.currentTimeMillis() - start;
        log.info("[SERVICE] searchEs fallback success cost={}ms keyResult=source=DB,total={}",
                cost, fallback.getTotalElements());
        return fallback;
    }

    private Page<OperationLogEntity> searchFallbackPage(String keyword, Long startTime, Long endTime, int page, int size) {
        LocalDateTime start = toLocalDateTime(startTime);
        LocalDateTime end = toLocalDateTime(endTime);

        List<OperationLogEntity> allResults = operationLogRepository.searchForEsFallback(keyword, start, end);
        AuditContext.put("logSearchDbMatched", allResults.size());

        int total = allResults.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<OperationLogEntity> pageContent = fromIndex < toIndex
                ? allResults.subList(fromIndex, toIndex)
                : List.of();

        return new PageImpl<>(
                pageContent,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")),
                total
        );
    }

    private void markSource(Page<OperationLogEntity> page, String source) {
        if (page == null || page.getContent() == null) {
            return;
        }
        // 只是回傳給前端看的暫存欄位，不會落 DB。
        page.getContent().forEach(log -> log.setQuerySource(source));
    }

    private void markStaleFallbackSource(Page<OperationLogEntity> dbPage) {
        if (dbPage == null || dbPage.getContent() == null) {
            return;
        }

        List<Long> ids = dbPage.getContent().stream()
                .map(OperationLogEntity::getId)
                .filter(id -> id != null)
                .toList();

        try {
            Set<Long> existingIds = operationLogEsService.findExistingIds(ids);
            List<OperationLogEntity> missingLogs = new ArrayList<>();
            dbPage.getContent().forEach(log -> {
                if (log.getId() != null && existingIds.contains(log.getId())) {
                    log.setQuerySource("ES");
                } else {
                    log.setQuerySource("ES->DB");
                    missingLogs.add(log);
                }
            });
            sendMissingLogsToMq(missingLogs);
        } catch (Exception e) {
            log.warn("[SERVICE] check ES existing ids failed, mark fallback rows as ES->DB: {}", e.getMessage());
            markSource(dbPage, "ES->DB");
            sendMissingLogsToMq(dbPage.getContent());
        }
    }

    private void sendMissingLogsToMq(List<OperationLogEntity> missingLogs) {
        if (missingLogs == null || missingLogs.isEmpty()) {
            return;
        }

        try {
            mqTaskExecutor.execute(() -> {
                int sent = 0;
                for (OperationLogEntity logEntity : missingLogs) {
                    eventLogService.sendOperationLog(logEntity);
                    sent++;
                }
                log.info("[SERVICE] searchEs fallback sent missing logs to MQ count={}", sent);
            });
            AuditContext.put("logSearchMqResend", missingLogs.size());
        } catch (Exception e) {
            AuditContext.put("logSearchMqResend", "FAILED");
            log.warn("[SERVICE] searchEs fallback submit MQ compensation failed: {}", e.getMessage());
        }
    }

    private boolean shouldUseDbFallback(Page<OperationLogEntity> esResult, Page<OperationLogEntity> dbResult) {
        if (dbResult == null || dbResult.isEmpty()) {
            return false;
        }
        if (esResult == null || esResult.isEmpty()) {
            return true;
        }

        List<Long> dbIds = dbResult.getContent().stream()
                .map(OperationLogEntity::getId)
                .filter(id -> id != null)
                .toList();
        try {
            Set<Long> existingIds = operationLogEsService.findExistingIds(dbIds);
            boolean missingAny = dbIds.stream().anyMatch(id -> !existingIds.contains(id));
            log.info("[SERVICE] searchEs stale check pageIds={}, existingInEs={}, missingAny={}",
                    dbIds.size(), existingIds.size(), missingAny);
            return missingAny;
        } catch (Exception e) {
            log.warn("[SERVICE] check ES stale failed, keep ES result: {}", e.getMessage());
            return false;
        }
    }

}
