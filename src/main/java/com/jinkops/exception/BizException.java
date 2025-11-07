package com.jinkops.exception;


//可预期的业务问题
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;


    public BizException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + "：" + detail);
        //把错误信息传给父类（方便打印日志）
        this.errorCode = errorCode;
    }
    //简易抛错
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }


    public ErrorCode getErrorCode() {
        return errorCode;
    }


}
