package com.jinkops.controller;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.entity.user.Permission;
import com.jinkops.service.PermissionService;
import com.jinkops.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    // 權限列表
    @RequirePermission("sys:perm:list")
    @GetMapping
    public ApiResponse<List<Permission>> list() {
        log.info("[API] GET /api/permissions");
        return ApiResponse.success(permissionService.list());
    }

    // 新增權限
    @RequirePermission("sys:perm:create")
    @PostMapping
    public ApiResponse<Permission> create(@RequestBody PermissionCreateRequest request) {
        log.info("[API] POST /api/permissions");
        return ApiResponse.success(permissionService.create(request.getCode()));
    }

    // 刪除權限
    @RequirePermission("sys:perm:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        log.info("[API] DELETE /api/permissions/{id}");
        permissionService.delete(id);
        return ApiResponse.success("ok", null);
    }

    public static class PermissionCreateRequest {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
