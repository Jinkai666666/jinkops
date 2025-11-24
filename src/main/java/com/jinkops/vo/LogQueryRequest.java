package com.jinkops.vo;

import lombok.Data;

// 日志分页查询入参
@Data
public class LogQueryRequest {

    // 页码，从 1 开始
    private Integer page = 0;

    // 每页大小
    private Integer size = 10;

    // 模糊搜索字段（用户名/模块/路径）
    private String keyword;

    // 起始时间（字符串，前端传 yyyy-MM-ddTHH:mm:ss）
    private String startTime;

    // 结束时间
    private String endTime;
}
