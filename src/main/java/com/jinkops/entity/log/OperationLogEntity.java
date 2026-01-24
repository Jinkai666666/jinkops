package com.jinkops.entity.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日誌實體類
 * 用於存儲每次帶 @OperationLog 註解的方法呼叫資訊
 */
@Entity
@Table(name = "operation_log")
@Data
public class OperationLogEntity {

    public OperationLogEntity() {}

    // 主鍵自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 操作人用戶名
    @Column(length = 64)
    private String username;

    // 操作名稱或類型（"新增用戶"、"刪除日誌" 等）
    @Column(length = 128)
    private String operation;

    // traceId 用於日誌全鏈路追蹤
    @Column(nullable = false, length = 64)
    private String traceId;

    // 類名
    @Column(nullable = false, length = 128)
    private String className;

    // 方法
    @Column(nullable = false, length = 128)
    private String methodName;

    // 方法參數（JSON 化後的字串）
    @Column(columnDefinition = "TEXT")
    private String args;

    // 註解描述
    @Column(length = 255)
    private String description;

    // 耗時（毫秒）
    @Column(nullable = false)
    private Long elapsedTime;

    // 記錄時間（對應 DB 欄位 create_time）
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    // 路徑
    @Column(length = 255)
    private String uri;

    // HTTP 方法
    @Column(length = 16)
    private String httpMethod;

    // IP
    @Column(length = 64)
    private String ip;
}
