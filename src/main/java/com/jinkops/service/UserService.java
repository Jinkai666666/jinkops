package com.jinkops.service;

import com.jinkops.entity.User;
import com.jinkops.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    // 注入 repository
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 所有用户
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 用户名查
    public User getByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
