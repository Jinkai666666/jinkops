package com.jinkops.exception;

import lombok.Getter;

//系統內統一錯誤碼
 //每個錯誤編號和說明
@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    USER_NOT_FOUND(404, "用戶不存在"),
    USER_EXIST(409, "用戶已存在"),
    SYSTEM_BUSY(429, "系統繁忙，請稍後再試"),

    UNAUTHORIZED(401, "未登錄或token無效"),
    FORBIDDEN(403, "無權限訪問"),
    BAD_REQUEST(400, "請求參數錯誤"),
    INTERNAL_ERROR(500, "服務器異常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
