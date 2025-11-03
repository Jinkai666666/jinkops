package com.jinkops.controller;


import com.jinkops.entity.OperationLogEntity;
import com.jinkops.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

//操作日志 控制器
//日志分页咨询
@RestController
@RequestMapping("/api/logs")
public class OperrationLogController {

    private final OperationLogService operationLogService;

    public OperrationLogController(OperationLogService operationService){
        this.operationLogService = operationService;
    }

    //分页查询操作日志
    //GET /api/logs?page=0&size=10
    @GetMapping
    public Page<OperationLogEntity> getLogs(Pageable pageable){
        return operationLogService.getLogs(pageable);
    }
}
