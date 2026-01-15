package com.jinkops.controller;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.service.RbacService;
import com.jinkops.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    // 指派用戶角色
    @RequirePermission("sys:rbac:assign")
    @PostMapping("/user-role/assign")
    public ApiResponse<String> assignUserRoles(@RequestBody UserRoleAssignRequest request) {
        log.info("[API] POST /api/rbac/user-role/assign");
        rbacService.assignUserRoles(request.getUserId(), request.getRoleIds());
        return ApiResponse.success("ok", null);
    }

    // 指派角色權限
    @RequirePermission("sys:rbac:assign")
    @PostMapping("/role-permission/assign")
    public ApiResponse<String> assignRolePermissions(@RequestBody RolePermissionAssignRequest request) {
        log.info("[API] POST /api/rbac/role-permission/assign");
        rbacService.assignRolePermissions(request.getRoleId(), request.getPermissionIds());
        return ApiResponse.success("ok", null);
    }

    public static class UserRoleAssignRequest {
        private Long userId;
        private Set<Long> roleIds = new HashSet<>();

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Set<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(Set<Long> roleIds) {
            this.roleIds = roleIds;
        }
    }

    public static class RolePermissionAssignRequest {
        private Long roleId;
        private Set<Long> permissionIds = new HashSet<>();

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public Set<Long> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(Set<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }
}
