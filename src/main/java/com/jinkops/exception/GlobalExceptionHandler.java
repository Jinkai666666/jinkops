package com.jinkops.exception;

import com.jinkops.vo.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 全域異常處理
// 負責統一攔截專案中所有異常，並格式化輸出

@RestControllerAdvice
public class GlobalExceptionHandler {


    // 處理業務異常

    @ExceptionHandler(BizException.class)
    public ApiResponse<?> handleBizException(BizException e) {
        return ApiResponse.fail(e.getErrorCode());
    }


    // 處理其他未捕獲的異常

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        // 保留系統異常原始訊息給日誌，返回前端固定提示
        e.printStackTrace();
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR);
    }
}
