package com.jinkops.annotation;

import com.jinkops.enums.PermissionMode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 只能標在方法上，介面用的
@Retention(RetentionPolicy.RUNTIME) // 運行的時候 AOP 可讀取
@Documented
public @interface RequirePermission {

    String[] value(); // 支援多個權限

    PermissionMode mode() default PermissionMode.AND; // 默認 AND
}
