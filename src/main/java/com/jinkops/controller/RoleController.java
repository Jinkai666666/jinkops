package com.jinkops.controller;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.entity.user.Role;
import com.jinkops.service.RoleService;
import com.jinkops.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // 角色列表
    @RequirePermission("sys:role:list")
    @GetMapping
    public ApiResponse<List<Role>> list() {
        log.info("[API] GET /api/roles");
        return ApiResponse.success(roleService.list());
    }

    // 新增角色
    @RequirePermission("sys:role:create")
    @PostMapping
    public ApiResponse<Role> create(@RequestBody RoleCreateRequest request) {
        log.info("[API] POST /api/roles");
        return ApiResponse.success(roleService.create(request.getCode()));
    }

    // 更新角色
    @RequirePermission("sys:role:update")
    @PutMapping("/{id}")
    public ApiResponse<Role> update(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        log.info("[API] PUT /api/roles/{id}");
        return ApiResponse.success(roleService.update(id, request.getCode()));
    }

    // 刪除角色
    @RequirePermission("sys:role:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        log.info("[API] DELETE /api/roles/{id}");
        roleService.delete(id);
        return ApiResponse.success("ok", null);
    }

    public static class RoleCreateRequest {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class RoleUpdateRequest {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
