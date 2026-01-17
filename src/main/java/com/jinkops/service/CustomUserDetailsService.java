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

    // 建構注入，避免循環依賴
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    // 這裡把角色/權限攤平給 Security 用
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("帳號不存在");
        }

        // 角色及權限全部展開給予 Security
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        for (Role role : user.getRoles()) {
            String roleCode = role.getCode() == null ? "" : role.getCode().trim().toUpperCase();
            // 角色本身當成權限給 Security（統一大寫避免大小寫/空白問題）
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));

            // 把角色下面的每個權限加進去
            for (Permission p : role.getPermissions()) {
                String permCode = p.getCode() == null ? "" : p.getCode().trim().toUpperCase();
                authorities.add(new SimpleGrantedAuthority(permCode));
            }
        }

        // 返回給 Security
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
