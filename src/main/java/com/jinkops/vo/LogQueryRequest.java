package com.jinkops.vo;

import lombok.Data;

// 日誌分頁查詢入參
@Data
public class LogQueryRequest {

    // 頁碼，從 1 開始
    private Integer page = 0;

    // 每頁大小
    private Integer size = 10;

    // 模糊搜尋欄位（用戶名/模組/路徑）
    private String keyword;

    // 起始時間（字串，前端傳 yyyy-MM-ddTHH:mm:ss）
    private String startTime;

    // 結束時間
    private String endTime;
}
