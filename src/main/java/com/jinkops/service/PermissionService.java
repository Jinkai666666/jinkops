package com.jinkops.service;

import com.jinkops.cache.service.PermissionCache;
import com.jinkops.entity.user.Permission;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionCache permissionCache;

    // 權限管理列表
    public List<Permission> list() {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] listPermissions start keyParams=none");
        try {
            List<Permission> result = permissionRepository.findAll();
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] listPermissions success cost={}ms keyResult=count={}", cost, result.size());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] listPermissions failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 建一個新權限
    @Transactional
    public Permission create(String code) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] createPermission start keyParams=code={}", code);
        try {
            Permission permission = new Permission();
            permission.setCode(code);
            Permission saved = permissionRepository.save(permission);
            permissionCache.deleteAll();
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] createPermission success cost={}ms keyResult=permissionId={}", cost, saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("[SERVICE] createPermission failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 刪除權限
    @Transactional
    public void delete(Long id) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] deletePermission start keyParams=id={}", id);
        try {
            Permission permission = permissionRepository.findById(id)
                    .orElseThrow(() -> new BizException(ErrorCode.PERMISSION_NOT_FOUND));
            permissionRepository.delete(permission);
            permissionCache.deleteAll();
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] deletePermission success cost={}ms keyResult=ok", cost);
        } catch (Exception e) {
            log.error("[SERVICE] deletePermission failed reason={}", e.getMessage(), e);
            throw e;
        }
    }
}
