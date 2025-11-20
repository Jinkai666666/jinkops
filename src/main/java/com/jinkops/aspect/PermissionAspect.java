package com.jinkops.aspect;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.enums.PermissionMode;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class PermissionAspect {
    // 拦截所有带 @RequirePermission 的方法
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        // 从注解拿权限码
        String[] requiredPermissions = requirePermission.value();
        PermissionMode mode = requirePermission.mode();
        //从Security 拿到当前用户
        Authentication auth =  SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            //没登录 401
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        //拿到用户权限集
        Set<String> userPerm = auth.getAuthorities()
                .stream()
                //转换成字符串
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());


        //判断权限（AND / OR）
        boolean allowed;

        if (mode == PermissionMode.AND) {
            // 所有 requiredPermissions 用户都必须拥有
            allowed = userPerm.containsAll(Set.of(requiredPermissions));
        } else {
            // OR：只要用户有其中一个权限即可
            allowed = false;
            for (String p : requiredPermissions) {
                if (userPerm.contains(p)) {
                    allowed = true;
                    break;
                }
            }
        }

        // 如果不满足权限：抛异常
        if (!allowed) {
            log.warn("Permission denied. required={}, userPerm={}", requiredPermissions, userPerm);
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }



}
