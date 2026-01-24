package com.jinkops.service;

import com.jinkops.entity.user.Permission;
import com.jinkops.entity.user.Role;
import com.jinkops.entity.user.User;
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

    // 建構子注入，確保有資料來源
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    // 將角色／權限轉成 Security 需要的格式
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("使用者不存在");
        }

        // 角色與權限全部展示給 Security
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        for (Role role : user.getRoles()) {
            String roleCode = role.getCode() == null ? "" : role.getCode().trim().toUpperCase();
            // 角色本身也要當作權限丟給 Security（統一大寫避免大小寫差異）
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));

            // 把角色下的每個權限加入
            for (Permission p : role.getPermissions()) {
                String permCode = p.getCode() == null ? "" : p.getCode().trim().toUpperCase();
                authorities.add(new SimpleGrantedAuthority(permCode));
            }
        }

        // 回傳給 Security
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
