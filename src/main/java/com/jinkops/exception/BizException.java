package com.jinkops.exception;

// 可預期的業務問題
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + "：" + detail);
        // 把錯誤訊息傳給父類（方便列印日誌）
        this.errorCode = errorCode;
    }

    // 簡易拋錯
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
