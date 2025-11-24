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

    @Operation(summary = "日志统一分页查询（keyword + 时间区间 + 分页）")
    @PostMapping("/page")
    public Page<OperationLogEntity> page(@RequestBody LogQueryRequest req) {

        Pageable pageable = PageRequest.of(
                req.getPage(),
                req.getSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        // keyword 优先
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            return operationLogService.searchLogs(req.getKeyword(), pageable);
        }

        // 时间区间过滤
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

        // 默认查全部
        return operationLogService.getLogs(pageable);
    }


}
