package com.jinkops.annotation;

import com.jinkops.enums.PermissionMode;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 只能标在方法上，接口用的
@Retention(RetentionPolicy.RUNTIME) // 运行的时候 AOP可读取
@Documented
public @interface RequirePermission {


    String[] value(); // 支持多个权限

    PermissionMode mode() default PermissionMode.AND; // 默认 AND}
}