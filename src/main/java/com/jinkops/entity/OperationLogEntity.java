package com.jinkops.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 用于存储每次带 @OperationLog 注解的方法调用信息
 */
@Entity
@Table(name = "operation_log")
@Data
public class OperationLogEntity {

    public OperationLogEntity() {}

    // 主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // traceId 用于日志全链路追踪
    @Column(nullable = false, length = 64)
    private String traceId;

    // 类名
    @Column(nullable = false, length = 128)
    private String className;

    // 方法
    @Column(nullable = false, length = 128)
    private String methodName;

    // 方法参数（JSON化后的字符串）
    @Column(columnDefinition = "TEXT")
    private String args;

    // 注解描述
    @Column(length = 255)
    private String description;

    // 耗时（毫秒）
    @Column(nullable = false)
    private Long elapsedTime;

    // 记录时间
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
