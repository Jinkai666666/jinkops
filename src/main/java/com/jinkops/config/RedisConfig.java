package com.jinkops.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ComponentScan(basePackages = "com.jinkops")
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 新建一个 RedisTemplate
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 连接
        template.setConnectionFactory(factory);

        // 序列化，存进去前转格式
        //序列化成字符串
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        // value 转成 JSON
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        // key 字符格式存
        template.setKeySerializer(keySerializer);
        // value JSON 格式存
        template.setValueSerializer(valueSerializer);
        // hash 的 key 也用字符串（比如 redis 的 map）
        template.setHashKeySerializer(keySerializer);
        // hash 的 value 也用 JSON
        template.setHashValueSerializer(valueSerializer);

        //初始化配置生效
        template.afterPropertiesSet();  
        return template;
    }
}
