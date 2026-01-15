package com.jinkops.service;

import com.jinkops.entity.user.Role;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    // 角色管理列表
    public List<Role> list() {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] listRoles start keyParams=none");
        try {
            List<Role> result = roleRepository.findAll();
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] listRoles success cost={}ms keyResult=count={}", cost, result.size());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] listRoles failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 建一個新角色
    public Role create(String code) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] createRole start keyParams=code={}", code);
        try {
            Role role = new Role();
            role.setCode(code);
            Role saved = roleRepository.save(role);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] createRole success cost={}ms keyResult=roleId={}", cost, saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("[SERVICE] createRole failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 更新角色基本資訊
    public Role update(Long id, String code) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] updateRole start keyParams=id={},code={}", id, code);
        try {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new BizException(ErrorCode.ROLE_NOT_FOUND));
            role.setCode(code);
            Role saved = roleRepository.save(role);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] updateRole success cost={}ms keyResult=roleId={}", cost, saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("[SERVICE] updateRole failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 刪除角色
    public void delete(Long id) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] deleteRole start keyParams=id={}", id);
        try {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new BizException(ErrorCode.ROLE_NOT_FOUND));
            roleRepository.delete(role);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] deleteRole success cost={}ms keyResult=ok", cost);
        } catch (Exception e) {
            log.error("[SERVICE] deleteRole failed reason={}", e.getMessage(), e);
            throw e;
        }
    }
}
