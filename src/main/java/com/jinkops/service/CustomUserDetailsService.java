package com.jinkops.service;

import com.jinkops.entity.Permission;
import com.jinkops.entity.Role;
import com.jinkops.entity.User;
import com.jinkops.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // 构造注入，避免循环依赖
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在");
        }

        // 角色及权限全部展开 给予Security
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        for (Role role : user.getRoles()) {
            // 角色本身当成权限给 Security
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));

            // 把角色下面的每个权限加进去
            for (Permission p : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(p.getCode()));
            }
        }

        // 返回给 Security
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

}
