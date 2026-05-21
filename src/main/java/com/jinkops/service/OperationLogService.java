package com.jinkops.service;

import com.jinkops.audit.AuditContext;
import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.mq.producer.EventLogService;
import com.jinkops.repository.OperationLogRepository;
import com.jinkops.service.es.OperationLogEsService;
import com.jinkops.vo.LogQueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

// 操作日誌查詢服務，這裡負責 DB 分頁、ES 優先搜尋、以及 ES 失效時的 DB 兜底。
@Service
@Slf4j
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final OperationLogEsService operationLogEsService;
    private final EventLogService eventLogService;

    // 日誌列表查詢，按建立時間倒序。
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

    // 關鍵詞模糊查詢，這個是普通 DB 查詢入口。
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

    // 時間區間查詢，還是直接走 DB，適合穩定分頁。
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

    // 統一查詢入口，避免 Controller 自己分流。
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

    // 進階搜尋：先查 ES；ES 沒命中或掛掉，才回 DB 兜底。
    public Page<OperationLogEntity> searchEs(String keyword, Long startTime, Long endTime, int page, int size) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] searchEs start keyParams=keyword={},start={},end={},page={},size={}",
                keyword, startTime, endTime, page, size);
        try {
            Page<OperationLogEntity> esResult = operationLogEsService.search(keyword, startTime, endTime, page, size);
            if (esResult != null && !esResult.isEmpty()) {
                // ES 有資料就直接回，這裡就是「優先走 ES」的主路徑。
                AuditContext.put("logSearchSource", "ES");
                AuditContext.put("logSearchFallback", "NO");
                long cost = System.currentTimeMillis() - start;
                log.info("[SERVICE] searchEs success cost={}ms keyResult=source=ES,total={}",
                        cost, esResult.getTotalElements());
                return esResult;
            }

            // ES 查了但空，後面走 DB，順便補一份到 MQ 讓 ES 慢慢補齊。
            AuditContext.put("logSearchSource", "ES_EMPTY");
        } catch (Exception e) {
            // ES 真掛了也不要影響查詢，回 DB 兜底。
            AuditContext.put("logSearchSource", "ES_ERROR");
            log.error("[SERVICE] searchEs failed on ES query reason={}", e.getMessage(), e);
        }

        Page<OperationLogEntity> fallback = searchFallbackPage(keyword, startTime, endTime, page, size);
        AuditContext.put("logSearchFallback", "DB");
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
        if (!allResults.isEmpty()) {
            sendFallbackResultsToMq(allResults);
        }

        int total = allResults.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<OperationLogEntity> pageContent = fromIndex < toIndex
                ? allResults.subList(fromIndex, toIndex)
                : List.of();

        return new PageImpl<>(pageContent, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")), total);
    }

    private void sendFallbackResultsToMq(List<OperationLogEntity> allResults) {
        try {
            // DB 兜底查到的資料不直接寫 ES，改丟 MQ；後面 OperationLogListener 會自動消費並寫入 ES。
            for (OperationLogEntity entity : allResults) {
                eventLogService.sendOperationLog(entity);
            }
            AuditContext.put("logSearchMqResend", allResults.size());
            log.info("[SERVICE] searchEs fallback sent operation logs to MQ count={}", allResults.size());
        } catch (Exception e) {
            AuditContext.put("logSearchMqResend", "FAILED");
            log.warn("[SERVICE] searchEs fallback send to MQ failed: {}", e.getMessage(), e);
        }
    }
}
