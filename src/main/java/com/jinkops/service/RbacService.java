package com.jinkops.service;

import com.jinkops.entity.user.Permission;
import com.jinkops.entity.user.Role;
import com.jinkops.entity.user.User;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.repository.PermissionRepository;
import com.jinkops.repository.RoleRepository;
import com.jinkops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RbacService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    // 綁定用戶與角色，這裡只管關聯，不做其他業務
    public void assignUserRoles(Long userId, Set<Long> roleIds) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] assignUserRoles start keyParams=userId={},roleCount={}",
                userId, roleIds.size());
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

            List<Role> roles = roleRepository.findAllById(roleIds);
            if (roles.size() != roleIds.size()) {
                // 有角色不存在就直接拒絕
                throw new BizException(ErrorCode.ROLE_NOT_FOUND);
            }

            user.setRoles(new HashSet<>(roles));
            userRepository.save(user);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] assignUserRoles success cost={}ms keyResult=ok", cost);
        } catch (Exception e) {
            log.error("[SERVICE] assignUserRoles failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 綁定角色與權限，保持最小管理能力
    public void assignRolePermissions(Long roleId, Set<Long> permissionIds) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] assignRolePermissions start keyParams=roleId={},permCount={}",
                roleId, permissionIds.size());
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new BizException(ErrorCode.ROLE_NOT_FOUND));

            List<Permission> perms = permissionRepository.findAllById(permissionIds);
            if (perms.size() != permissionIds.size()) {
                // 有權限不存在就直接拒絕
                throw new BizException(ErrorCode.PERMISSION_NOT_FOUND);
            }

            role.setPermissions(new HashSet<>(perms));
            roleRepository.save(role);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] assignRolePermissions success cost={}ms keyResult=ok", cost);
        } catch (Exception e) {
            log.error("[SERVICE] assignRolePermissions failed reason={}", e.getMessage(), e);
            throw e;
        }
    }
}
