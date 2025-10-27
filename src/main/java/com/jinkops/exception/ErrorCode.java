package com.jinkops.exception;

import lombok.Getter;

//系统内统一错误码
 //每个错误编号和说明
@Getter
public enum ErrorCode {

    USER_NOT_FOUND(404, "用户不存在"),
    INTERNAL_ERROR(500, "服务器异常");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
