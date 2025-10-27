package com.jinkops.exception;

import com.jinkops.vo.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//全局异常处理
//负责统一拦截项目中所有异常，并格式化输出

@RestControllerAdvice
public class GlobalExceptionHandler {


    //处理业务异常

    @ExceptionHandler(BizException.class)
    public ApiResponse<?> handleBizException(BizException e) {
        return ApiResponse.fail(e.getErrorCode().getCode(), e.getMessage());
    }


    //处理其他未捕获的异常

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        return ApiResponse.fail(500, "系统异常：" + e.getMessage());
    }
}
