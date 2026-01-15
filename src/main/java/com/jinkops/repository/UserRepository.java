package com.jinkops.repository;

import com.jinkops.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

// 繼承 JpaRepository<User, Long> 自動生成增刪改查
public interface UserRepository extends JpaRepository<User, Long> {

    // 按用戶名查
    User findByUsername(String username);
    // 刪除用戶
    void deleteUserByUsername(String username);
}
