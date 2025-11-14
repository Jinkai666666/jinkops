package com.jinkops.config;

import com.jinkops.entity.user.Permission;
import com.jinkops.entity.user.Role;
import com.jinkops.entity.user.User;
import com.jinkops.repository.PermissionRepository;
import com.jinkops.repository.RoleRepository;
import com.jinkops.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class RbacDataInitializer {

    @Bean
    public CommandLineRunner initRbacData(UserRepository userRepo,
                                          RoleRepository roleRepo,
                                          PermissionRepository permRepo,
                                          PasswordEncoder encoder) {

        return args -> {

            // 如果用户表有数据，直接跳过
            if (userRepo.count() > 0) {
                return;
            }

            // 创建权限
            Permission p1 = new Permission();
            p1.setCode("sys:user:list");

            Permission p2 = new Permission();
            p2.setCode("sys:user:update");

            permRepo.save(p1);
            permRepo.save(p2);

            // 创建角色
            Role admin = new Role();
            admin.setCode("ADMIN");
            admin.setPermissions(Set.of(p1, p2));

            Role normal = new Role();
            normal.setCode("USER");
            normal.setPermissions(Set.of(p1)); // 普通用户只有查询权限

            roleRepo.save(admin);
            roleRepo.save(normal);

            // 创建一个管理员账号
            User u1 = new User();
            u1.setUsername("admin");
            u1.setPassword(encoder.encode("123456"));
            u1.setEmail("admin@test.com");
            u1.setRoles(Set.of(admin));

            userRepo.save(u1);

            // 创建普通用户
            User u2 = new User();
            u2.setUsername("user");
            u2.setPassword(encoder.encode("123456"));
            u2.setEmail("user@test.com");
            u2.setRoles(Set.of(normal));

            userRepo.save(u2);
        };
    }
}
