package com.jinkops.controller;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.mq.producer.EventLogService;
import com.jinkops.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/mq")
@RequiredArgsConstructor
public class RabbitMqController {

    private final EventLogService eventLogService;

    @PostMapping("/operation-log")
    public ApiResponse<OperationLogEntity> sendOperationLog(@RequestBody OperationLogEntity entity) {
        if (entity.getTraceId() == null || entity.getTraceId().isBlank()) {
            entity.setTraceId(UUID.randomUUID().toString());
        }
        if (entity.getTimestamp() == null) {
            entity.setTimestamp(LocalDateTime.now());
        }

        eventLogService.sendOperationLog(entity);
        return ApiResponse.success("pushed to ops.event.log.queue", entity);
    }
}
