package com.jinkops.config;

import com.jinkops.service.CustomUserDetailsService;
import com.jinkops.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.jinkops.web.security.JwtAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@EnableMethodSecurity //開啓 @PreAuthorize
@Configuration //配置類
@EnableWebSecurity
@ComponentScan(basePackages = "com.jinkops")
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    private final JwtAuthenticationFilter jwtFilter;
    
    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;
// 構造注入


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .formLogin(form -> form.disable())
                    .httpBasic(basic -> basic.disable());
            return http.build();
        }

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**",
                                "/doc.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                 "/webjars/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/test/redisson/*").permitAll()
                        .requestMatchers("/test/mq").permitAll()
                        .requestMatchers("/api/mq/operation-log").permitAll()
                        .requestMatchers("/api/lock/user/**").permitAll()
                        .requestMatchers("/api/logs/page").permitAll()// ??????????????????

                        .anyRequest().authenticated()            // ?????????????????????
                )
                .formLogin(form -> form.disable())  // ?????????????????????
                .httpBasic(basic -> basic.disable()); // ??????????????????
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();// 密碼加密器
    }

    // 註冊認證提供者（指定用戶查找邏輯 + 密碼校驗邏輯）
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService); // ← 用 RBAC 的 UserDetailsService
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
    // 暴露認證管理器（登錄認證的總入口，讓 Controller 可以直接調用認證）
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
