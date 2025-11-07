package com.jinkops.exception;

import lombok.Getter;

//系统内统一错误码
 //每个错误编号和说明
@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    USER_NOT_FOUND(404, "用户不存在"),
    UNAUTHORIZED(401, "未登录或token无效"),
    FORBIDDEN(403, "无权限访问"),
    BAD_REQUEST(400, "请求参数错误"),
    INTERNAL_ERROR(500, "服务器异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
