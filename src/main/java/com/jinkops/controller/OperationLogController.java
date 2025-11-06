package com.jinkops.controller;

import com.jinkops.entity.OperationLogEntity;
import com.jinkops.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

// 操作日志控制器
@Tag(name = "操作日志接口", description = "提供日志查询与搜索功能")
@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    // 分页查询操作日志
    @Operation(summary = "分页查询操作日志（按时间倒序）")
    @GetMapping
    public Page<OperationLogEntity> getLogs(Pageable pageable) {
        return operationLogService.getLogs(pageable);
    }

    @Operation(summary = "模糊搜索日志（按时间倒序）")
    // 模糊搜索日志
    @GetMapping("/search")
    public Page<OperationLogEntity> searchLogs(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return operationLogService.searchLogs(keyword, pageable);
    }
}
