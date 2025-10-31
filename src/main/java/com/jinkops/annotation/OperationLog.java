package com.jinkops.annotation;

import java.lang.annotation.*;

/**
 * 自定义操作日志注解
 * 用于标记需要自动记录日志的方法
 */
@Target(ElementType.METHOD)  // 作用于方法
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
@Documented
public @interface OperationLog {

    String value() default ""; // 日志描述，"用户登录" ."查询用户列表"
}
