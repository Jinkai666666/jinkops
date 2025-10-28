package com.jinkops.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user") // 数据表：user
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自增
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // 用户名

    @Column(nullable = false)
    private String password; // 密码

    private String email; // 邮箱
    private String role;  // 角色

    // getter/setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
