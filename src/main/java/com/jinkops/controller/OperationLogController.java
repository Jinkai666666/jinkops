package com.jinkops.controller;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.service.OperationLogService;
import com.jinkops.vo.ApiResponse;
import com.jinkops.vo.LogQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "操作日志接口", description = "提供日志查询与搜索功能")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "普通查询：POST /api/logs/page")
    @RequirePermission("sys:log:query")
    @PostMapping("/page")
    public ApiResponse<Page<OperationLogEntity>> page(@RequestBody LogQueryRequest req) {
        log.info("[API] POST /api/logs/page");
        return ApiResponse.success(operationLogService.page(req));
    }

    @Operation(summary = "ES 搜索：GET /api/logs/search/advanced")
    @RequirePermission("sys:log:query")
    @GetMapping("/search/advanced")
    public ApiResponse<Page<OperationLogEntity>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[API] GET /api/logs/search/advanced");
        return ApiResponse.success(operationLogService.searchEs(keyword, startTime, endTime, page, size));
    }
}
