package com.jinkops.repository;

import com.jinkops.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// 数据访问层接口
public interface UserRepository extends JpaRepository<User, Long> {
}
