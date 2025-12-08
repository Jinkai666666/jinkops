package com.jinkops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinkops.cache.key.UserKeys;
import com.jinkops.cache.service.CacheService;
import com.jinkops.entity.user.User;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;



    ////随机 TTL 方法
    private int randomTtl() {
        return 3600 + (int)(Math.random() * 300); // 随机 3600~3900
    }

    // 注入 repository
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 所有用户
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 用户名查
    public User findByUsername(String username) {
        String key = UserKeys.userInfo(username);
        String json = cacheService.get(key);
        if (json == null) {
            log.info("user cache miss, key={}", key);
        } else {
            log.info("user cache hit, key={}", key);
        }
        // 空值缓存命中
        if ("null".equals(json)) {
            return null;
        }

        // 命中正常缓存
        if (json != null) {
            try {
                return objectMapper.readValue(json, User.class);
            } catch (Exception ignored) {}
        }

        //  缓存没有 查数据库
        User user = userRepository.findByUsername(username);

        //  数据库没查到  写空值缓存（防穿透）
        if (user == null) {
            cacheService.set(key, "null", 30);
            return null;
        }

        //  查到了 写正常缓存
        try {
            cacheService.set(key, objectMapper.writeValueAsString(user), randomTtl());
        } catch (Exception ignored) {}
        System.out.println(passwordEncoder.encode("你的密码"));

        return user;
    }

    //  新增用户
    public User addUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    //  删除用户
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在：" + username);
        }
        // 删除分页缓存
        cacheService.deleteByPrefix("user:list:page:");
        log.info("page cache cleared after delete");


        userRepository.delete(user);
    }

    //根据用户名找查
    public Optional<User>getById(Long id) {
        return  userRepository.findById(id);
    }

    //查缓存
    public User getUserFromCache(String username) {
       //规范化 key
        String key = UserKeys.userInfo(username);
        //Redis 拿值
        String json = cacheService.get(key);
        if (json == null) {
            return null;
        }
        if ("null".equals(json)) {
            return null; // 空值缓存
        }
        try{
            return objectMapper.readValue(json,User.class);
        }catch (Exception e){
            return null ;
        }
    }

    //置入缓存
    public void setUserCache(String username, User user) {
        try {
            String key = UserKeys.userInfo(username);
            String json = objectMapper.writeValueAsString(user);
            cacheService.set(key,json,randomTtl());
        }catch (Exception ignored){}
    }

    //更新用户
    public User updateUser(User user) {
        //是否存在
        User dbUser = userRepository.findByUsername(user.getUsername());
        if (dbUser == null) {
            throw  new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在：" + user.getUsername());

        }
        dbUser.setEmail(user.getEmail());
        dbUser.setPassword(passwordEncoder.encode(user.getPassword()));

        // 保存 DB
        User updated = userRepository.save(dbUser);


        // 删除用户缓存
        String key = UserKeys.userInfo(updated.getUsername());
        cacheService.delete(key);
        log.info("cache delete after update, key={}", key);

        // 删除分页缓存
        cacheService.deleteByPrefix("user:list:page:");
        log.info("page cache cleared after update");


        return updated;

    }
       // 分页查询用户缓存
       public Page<User> pageUsers(int page, int size) {

           String key = UserKeys.userListPage(page, size);
           //整页的page size
           String json = cacheService.get(key);

           // 有缓存直接返回
           if (json != null) {
               //已存在并日志返回
               log.info("page cache hit, key={}", key);
               try {
                   List<User> list = objectMapper.readValue(
                           json,// Redis 返回的 JSON 字符串
                           objectMapper.getTypeFactory().constructCollectionType(List.class, User.class)
                   );

                   //List 包成 Page
                   return new PageImpl<>(list);
               } catch (Exception ignored) {
               }
           }

           //// 无缓存  查数据库

           //page 页，大小 size 的分页
           PageRequest pr = PageRequest.of(page - 1, size);
           //取数据库当页数据
           Page<User> pageData = userRepository.findAll(pr);

           // 缓存 list
           try {
               //本页序列 存入redis
               String listJson = objectMapper.writeValueAsString(pageData.getContent());
               cacheService.set(key, listJson, randomTtl());
               log.info("page cache write, key={}", key);
           } catch (Exception ignored) {}


           //返回分页完整数据
           return pageData;
       }




}
