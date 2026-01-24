package com.jinkops.repository;

import com.jinkops.entity.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByCodeIgnoreCase(String code);
}
