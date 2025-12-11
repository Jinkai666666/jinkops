package com.jinkops.annotation;

import java.lang.annotation.*;

/**
 * 自定義操作日誌註解
 * 用於標記需要自動記錄日誌的方法
 */
@Target(ElementType.METHOD)  // 作用於方法
@Retention(RetentionPolicy.RUNTIME) // 運行時生效
@Documented
public @interface OperationLog {

    String value() default ""; // 日誌描述，"用戶登錄" ."查詢用戶列表"
}
