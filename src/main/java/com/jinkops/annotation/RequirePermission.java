package com.jinkops.annotation;

import com.jinkops.enums.PermissionMode;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 只能標在方法上，接口用的
@Retention(RetentionPolicy.RUNTIME) // 運行的時候 AOP可讀取
@Documented
public @interface RequirePermission {


    String[] value(); // 支持多個權限

    PermissionMode mode() default PermissionMode.AND; // 默認 AND}
}