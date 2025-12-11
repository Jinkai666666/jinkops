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

    // 攔截所有帶 @RequirePermission 的方法
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        // 從註解拿權限碼
        String[] requiredPermissions = requirePermission.value();
        PermissionMode mode = requirePermission.mode();
        //從Security 拿到當前用戶
        Authentication auth =  SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            //沒登錄 401
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        //拿到用戶權限集
        Set<String> userPerm = auth.getAuthorities()
                .stream()
                //轉換成字符串
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());


        //判斷權限（AND / OR）
        boolean allowed;

        if (mode == PermissionMode.AND) {
            // 所有 requiredPermissions 用戶都必須擁有
            allowed = userPerm.containsAll(Set.of(requiredPermissions));
        } else {
            // OR：只要用戶有其中一個權限即可
            allowed = false;
            for (String p : requiredPermissions) {
                if (userPerm.contains(p)) {
                    allowed = true;
                    break;
                }
            }
        }

        // 如果不滿足權限：拋異常
        if (!allowed) {
            log.warn("Permission denied. required={}, userPerm={}", requiredPermissions, userPerm);
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }



}
