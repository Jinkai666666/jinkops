package com.jinkops.controller;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.jinkops.vo.LogQueryRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;

// 操作日誌控制器
@Tag(name = "操作日誌接口", description = "提供日誌查詢與搜索功能")
@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    // 分頁查詢操作日誌
    @Operation(summary = "分頁查詢操作日誌（按時間倒序）")
    @GetMapping
    public Page<OperationLogEntity> getLogs(Pageable pageable) {
        return operationLogService.getLogs(pageable);
    }

    @Operation(summary = "模糊搜索日誌（按時間倒序）")
    // 模糊搜索日誌
    @GetMapping("/search")
    public Page<OperationLogEntity> searchLogs(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return operationLogService.searchLogs(keyword, pageable);
    }

    @Operation(summary = "日誌統一分頁查詢（keyword + 時間區間 + 分頁）")
    @PostMapping("/page")
    public Page<OperationLogEntity> page(@RequestBody LogQueryRequest req) {

        Pageable pageable = PageRequest.of(
                req.getPage(),
                req.getSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        // keyword 優先
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            return operationLogService.searchLogs(req.getKeyword(), pageable);
        }

        // 時間區間過濾
        if (req.getStartTime() != null
                && !req.getStartTime().isBlank()
                && req.getEndTime() != null
                && !req.getEndTime().isBlank()) {



            return operationLogService.getLogsByTimeRange(
                    LocalDateTime.parse(req.getStartTime()),
                    LocalDateTime.parse(req.getEndTime()),
                    pageable
            );
        }

        // 默認查全部
        return operationLogService.getLogs(pageable);
    }


}
