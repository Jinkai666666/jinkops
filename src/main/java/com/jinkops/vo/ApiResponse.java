package com.jinkops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//统一接口返回格式，包含状态码、提示信息、数据
@Data
@NoArgsConstructor//自动生成空构造
@AllArgsConstructor//自动生成带全部参
public class ApiResponse<T> {

    private int code;     // 状态码
    private String msg;   // 提示信息
    private T data;       // 返回数据

    // 成功时返回
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    // 失败时返回
    public static <T> ApiResponse<T> fail(int code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
}
