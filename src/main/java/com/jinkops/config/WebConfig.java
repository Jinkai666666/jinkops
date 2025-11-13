package com.jinkops.config;

import com.jinkops.interceptor.JwtAuthInterceptor;
import com.jinkops.interceptor.TraceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// TraceInterceptor拦截所有请求
//全局 Web 配置：注册 Trace 与 JWT 拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {


    private TraceInterceptor traceInterceptor;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Autowired
    public WebConfig(TraceInterceptor traceInterceptor, JwtAuthInterceptor jwtAuthInterceptor) {
        this.traceInterceptor = traceInterceptor;
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Trace 日志链路拦截器
        registry.addInterceptor(traceInterceptor);
        //注册JWT拦截器（只拦 /api/**，登录注册接口除外）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/doc.html",
                        "/v3/**",
                        "/swagger-resources/**"
                );
    }
}
