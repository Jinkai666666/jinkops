package com.jinkops.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//統一接口返回格式，包含狀態碼、提示信息、數據
@Data
@NoArgsConstructor//自動生成空構造
@AllArgsConstructor//自動生成帶全部參
public class ApiResponse<T> {

    private int code;     // 狀態碼
    private String message;   // 提示信息
    private T data;       // 返回數據

    // 成功時返回
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    // 失敗時返回
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> fail(com.jinkops.exception.ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

}
