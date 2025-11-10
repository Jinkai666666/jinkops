package com.jinkops.repository;

import com.jinkops.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// 继承 JpaRepository<User, Long> 自动生成增删改查
public interface UserRepository extends JpaRepository<User, Long> {

    // 按用户名查
    User findByUsername(String username);
    //删除 用户
    void deleteUserByUsername(String Username);
}
