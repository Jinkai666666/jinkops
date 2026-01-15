package com.jinkops.controller;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.service.OperationLogService;
import com.jinkops.vo.ApiResponse;
import com.jinkops.vo.LogQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 操作日誌控制器
@Slf4j
@Tag(name = "操作日誌介面", description = "提供日誌查詢與搜尋功能")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    // 分頁查詢操作日誌
    @Operation(summary = "分頁查詢操作日誌（按時間倒序）")
    @GetMapping
    public ApiResponse<Page<OperationLogEntity>> getLogs(Pageable pageable) {
        log.info("[API] GET /api/logs");
        return ApiResponse.success(operationLogService.getLogs(pageable));
    }

    @Operation(summary = "模糊搜尋日誌（按時間倒序）")
    // 模糊搜尋日誌
    @GetMapping("/search")
    public ApiResponse<Page<OperationLogEntity>> searchLogs(
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        log.info("[API] GET /api/logs/search");
        return ApiResponse.success(operationLogService.searchLogs(keyword, pageable));
    }

    @Operation(summary = "日誌統一分頁查詢（keyword + 時間區間 + 分頁）")
    @PostMapping("/page")
    public ApiResponse<Page<OperationLogEntity>> page(@RequestBody LogQueryRequest req) {
        log.info("[API] POST /api/logs/page");
        return ApiResponse.success(operationLogService.page(req));
    }

    /**
     * 日誌搜尋（含降級查詢）
     */
    @GetMapping("/search/advanced")
    public ApiResponse<List<OperationLogEntity>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime
    ) {
        log.info("[API] GET /api/logs/search/advanced");
        return ApiResponse.success(operationLogService.searchEs(keyword, startTime, endTime));
    }
}
