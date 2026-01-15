package com.jinkops.config;

import com.jinkops.entity.user.Permission;
import com.jinkops.entity.user.Role;
import com.jinkops.entity.user.User;
import com.jinkops.repository.PermissionRepository;
import com.jinkops.repository.RoleRepository;
import com.jinkops.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@ComponentScan(basePackages = "com.jinkops")
public class RbacDataInitializer {

    @Bean
    public CommandLineRunner initRbacData(UserRepository userRepo,
                                          RoleRepository roleRepo,
                                          PermissionRepository permRepo,
                                          PasswordEncoder encoder) {

        return args -> {

            // 如果用戶表有資料，直接跳過
            if (userRepo.count() > 0) {
                return;
            }

            // 建立權限
            Permission p1 = new Permission();
            p1.setCode("sys:user:list");

            Permission p2 = new Permission();
            p2.setCode("sys:user:update");

            Permission pRoleList = new Permission();
            pRoleList.setCode("sys:role:list");
            Permission pRoleCreate = new Permission();
            pRoleCreate.setCode("sys:role:create");
            Permission pRoleUpdate = new Permission();
            pRoleUpdate.setCode("sys:role:update");
            Permission pRoleDelete = new Permission();
            pRoleDelete.setCode("sys:role:delete");

            Permission pPermList = new Permission();
            pPermList.setCode("sys:perm:list");
            Permission pPermCreate = new Permission();
            pPermCreate.setCode("sys:perm:create");
            Permission pPermDelete = new Permission();
            pPermDelete.setCode("sys:perm:delete");

            Permission pRbacAssign = new Permission();
            pRbacAssign.setCode("sys:rbac:assign");

            permRepo.save(p1);
            permRepo.save(p2);
            permRepo.save(pRoleList);
            permRepo.save(pRoleCreate);
            permRepo.save(pRoleUpdate);
            permRepo.save(pRoleDelete);
            permRepo.save(pPermList);
            permRepo.save(pPermCreate);
            permRepo.save(pPermDelete);
            permRepo.save(pRbacAssign);

            // 建立角色
            Role admin = new Role();
            admin.setCode("ADMIN");
            admin.setPermissions(Set.of(
                    p1, p2,
                    pRoleList, pRoleCreate, pRoleUpdate, pRoleDelete,
                    pPermList, pPermCreate, pPermDelete,
                    pRbacAssign
            ));

            Role normal = new Role();
            normal.setCode("USER");
            normal.setPermissions(Set.of(p1)); // 普通用戶只有查詢權限

            roleRepo.save(admin);
            roleRepo.save(normal);

            // 建立一個管理員帳號
            User u1 = new User();
            u1.setUsername("admin");
            u1.setPassword(encoder.encode("123456"));
            u1.setEmail("admin@test.com");
            u1.setRoles(Set.of(admin));

            userRepo.save(u1);

            // 建立普通用戶
            User u2 = new User();
            u2.setUsername("user");
            u2.setPassword(encoder.encode("123456"));
            u2.setEmail("user@test.com");
            u2.setRoles(Set.of(normal));

            userRepo.save(u2);
        };
    }
}
