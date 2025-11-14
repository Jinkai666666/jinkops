package com.jinkops.aspect;

import com.jinkops.annotation.RequirePermission;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;


@Aspect
@Component
public class PermissionAspect {
    // 拦截所有带 @RequirePermission 的方法
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        // 从注解拿权限码
        String required = requirePermission.value();
        //从Security 拿到当前用户
        Authentication auth =  SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            //没登录 401
            throw new RuntimeException("unauthorized");
        }

        //拿到用户权限集
        Set<String> userpermissions = auth.getAuthorities()
                .stream()
                //转换成字符串
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        //判断是否具备权限
        if (!userpermissions.contains(required)) {
            //403
            throw new RuntimeException("Forbidden: no permission " + required);
        }
    }



}
