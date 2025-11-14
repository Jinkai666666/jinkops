package com.jinkops.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 只能标在方法上，接口用的
@Retention(RetentionPolicy.RUNTIME) // 运行的时候 AOP可读取
@Documented
public @interface RequirePermission {

    // 权限码，例如 "sys:user:list"
    String value();
}
