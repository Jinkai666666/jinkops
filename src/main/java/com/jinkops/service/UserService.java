package com.jinkops.service;

import com.jinkops.entity.User;
import com.jinkops.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

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

    //  新增用户 (Create new user)
    public User addUser(User user) {
        return userRepository.save(user);
    }
    //  删除用户 (Delete by username)
    public void deleteUser(String username) {
        userRepository.deleteUserByUsername(username);
    }

    //根据用户名找查
    public Optional<User>getById(Long id) {
        return  userRepository.findById(id);
    }

    //Security 登录用
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User dbUser = userRepository.findByUsername(username);
        if (dbUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(dbUser.getUsername())
                .password(dbUser.getPassword())
                .authorities("USER") // 临时权限
                .build(); //  一个 UserDetails 对象
    }
}
