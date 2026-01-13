package com.jinkops.dto.log;

import java.time.LocalDateTime;

/**
 * OperationLog 的 ES 搜尋條件
 *
 * 只用在「查詢」，不參與任何寫入或業務邏輯
 * 單純就是前端傳條件進來用的
 */
public class OperationLogSearchRequest {

    /**
     * 關鍵字搜尋
     * 用來模糊比對操作內容、模組名稱之類的欄位
     */
    private String keyword;

    /**
     * 操作人帳號
     */
    private String username;

    /**
     * 查詢起始時間
     */
    private LocalDateTime startTime;

    /**
     * 查詢結束時間
     */
    private LocalDateTime endTime;

    /**
     * 第幾頁
     */
    private int page = 0;

    /**
     * 一頁幾筆資料
     */
    private int size = 10;

    // ===== getter / setter =====

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
