package com.jinkops.aspect;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.cache.service.PermissionCache;
import com.jinkops.enums.PermissionMode;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

// 只攔 RequirePermission，讓 RBAC 控制落在這裡
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionCache permissionCache;

    // 攔截所有帶 @RequirePermission 的方法
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        // 從註解拿權限碼，統一轉大寫避免大小寫不一致
        String[] requiredPermissions = requirePermission.value();
        for (int i = 0; i < requiredPermissions.length; i++) {
            requiredPermissions[i] = requiredPermissions[i].toUpperCase();
        }
        PermissionMode mode = requirePermission.mode();
        // 從 Security 拿到當前用戶
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            // 未登入直接拒絕
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        String username = auth.getName();

        // 先走 Redis 權限快取
        Set<String> userPerm = permissionCache.get(username);

        if (userPerm == null || userPerm.isEmpty()) {
            userPerm = auth.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
        }

        Set<String> normalizedPerm = userPerm.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(HashSet::new));

        // 超級管理員直接放行
        if (normalizedPerm.contains("ROLE_ADMIN")) {
            return;
        }

        // 判斷權限（AND / OR）
        boolean allowed;

        if (mode == PermissionMode.AND) {
            // AND：全部權限都要有
            allowed = normalizedPerm.containsAll(Set.of(requiredPermissions));
        } else {
            // OR：有一個就行
            allowed = false;
            for (String p : requiredPermissions) {
                if (normalizedPerm.contains(p)) {
                    allowed = true;
                    break;
                }
            }
        }

        // 如果不滿足權限：拋例外
        if (!allowed) {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String uri = attrs != null && attrs.getRequest() != null ? attrs.getRequest().getRequestURI() : "unknown";
            log.warn("Permission denied uri={} required={} granted={}", uri, Arrays.toString(requiredPermissions), normalizedPerm);
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
