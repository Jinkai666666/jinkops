package com.jinkops.controller;

import com.jinkops.entity.OperationLogEntity;
import com.jinkops.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

// 操作日志控制器
@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    // 分页查询操作日志
    @GetMapping
    public Page<OperationLogEntity> getLogs(Pageable pageable) {
        return operationLogService.getLogs(pageable);
    }

    // 模糊搜索日志
    @GetMapping("/search")
    public Page<OperationLogEntity> searchLogs(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return operationLogService.searchLogs(keyword, pageable);
    }
}
