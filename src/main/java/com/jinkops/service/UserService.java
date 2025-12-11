package com.jinkops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinkops.cache.key.UserKeys;
import com.jinkops.cache.service.CacheService;
import com.jinkops.entity.user.User;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.lock.LockService;
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
    private LockService lockService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;



    ////隨機 TTL 方法
    private int randomTtl() {
        return 3600 + (int)(Math.random() * 300); // 隨機 3600~3900
    }

    // 注入 repository
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 所有用戶
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 用戶名查
    public User findByUsername(String username) {
        String key = UserKeys.userInfo(username);
        String json = cacheService.get(key);
        if (json == null) {
            log.info("user cache miss, key={}", key);
        } else {
            log.info("user cache hit, key={}", key);
        }
        // 空值緩存命中
        if ("null".equals(json)) {
            return null;
        }

        // 命中正常緩存
        if (json != null) {
            try {
                return objectMapper.readValue(json, User.class);
            } catch (Exception ignored) {}
        }

        //  緩存沒有 查數據庫
        User user = userRepository.findByUsername(username);

        //  數據庫沒查到  寫空值緩存（防穿透）
        if (user == null) {
            cacheService.set(key, "null", 30);
            return null;
        }

        //  查到了 寫正常緩存
        try {
            cacheService.set(key, objectMapper.writeValueAsString(user), randomTtl());
        } catch (Exception ignored) {}
        System.out.println(passwordEncoder.encode("你的密碼"));

        return user;
    }

    //  新增用戶
    public User addUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    //  刪除用戶
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用戶不存在：" + username);
        }
        // 刪除分頁緩存
        cacheService.deleteByPrefix("user:list:page:");
        log.info("page cache cleared after delete");


        userRepository.delete(user);
    }

    //根據用戶名找查
    public Optional<User>getById(Long id) {
        return  userRepository.findById(id);
    }

    //查緩存
    public User getUserFromCache(String username) {
       //規範化 key
        String key = UserKeys.userInfo(username);
        //Redis 拿值
        String json = cacheService.get(key);
        if (json == null) {
            return null;
        }
        if ("null".equals(json)) {
            return null; // 空值緩存
        }
        try{
            return objectMapper.readValue(json,User.class);
        }catch (Exception e){
            return null ;
        }
    }

    //置入緩存
    public void setUserCache(String username, User user) {
        try {
            String key = UserKeys.userInfo(username);
            String json = objectMapper.writeValueAsString(user);
            cacheService.set(key,json,randomTtl());
        }catch (Exception ignored){}
    }

    //更新用戶
    public User updateUser(User user) {
        //是否存在
        User dbUser = userRepository.findByUsername(user.getUsername());
        if (dbUser == null) {
            throw  new BizException(ErrorCode.USER_NOT_FOUND, "用戶不存在：" + user.getUsername());

        }
        dbUser.setEmail(user.getEmail());
        dbUser.setPassword(passwordEncoder.encode(user.getPassword()));

        // 保存 DB
        User updated = userRepository.save(dbUser);


        // 刪除用戶緩存
        String key = UserKeys.userInfo(updated.getUsername());
        cacheService.delete(key);
        log.info("cache delete after update, key={}", key);

        // 刪除分頁緩存
        cacheService.deleteByPrefix("user:list:page:");
        log.info("page cache cleared after update");


        return updated;

    }
       // 分頁查詢用戶緩存
       public Page<User> pageUsers(int page, int size) {

           String key = UserKeys.userListPage(page, size);
           //整頁的page size
           String json = cacheService.get(key);

           // 有緩存直接返回
           if (json != null) {
               //已存在並日志返回
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

           //// 無緩存  查數據庫

           //page 頁，大小 size 的分頁
           PageRequest pr = PageRequest.of(page - 1, size);
           //取數據庫當頁數據
           Page<User> pageData = userRepository.findAll(pr);

           // 緩存 list
           try {
               //本頁序列 存入redis
               String listJson = objectMapper.writeValueAsString(pageData.getContent());
               cacheService.set(key, listJson, randomTtl());
               log.info("page cache write, key={}", key);
           } catch (Exception ignored) {}


           //返回分頁完整數據
           return pageData;
       }


    // 新增用户（带分布式锁 + 重复校验 + 缓存清理）
    public User createUser(User user) {

        String username = user.getUsername();
        String key = "lock:user:create:" + username;

        //  加锁
        String lockValue = lockService.tryLock(key, 5);
        if (lockValue == null) {
            throw new BizException(ErrorCode.SYSTEM_BUSY, "系统正在处理，请稍后再试");
        }

        try {
            // 判断是否重复
            User exist = userRepository.findByUsername(username);
            if (exist != null) {
                throw new BizException(ErrorCode.USER_EXIST, "用户名已存在");
            }

            //密码加密
            String rawPassword = user.getPassword();
            user.setPassword(passwordEncoder.encode(rawPassword));

            // 保存用户
            User saved = userRepository.save(user);

            // 5) 清缓存（因为有新用户）
            cacheService.deleteByPrefix("user:list:page:");
            log.info("page cache cleared after user create");

            return saved;

        } finally {
            // 解锁（
            lockService.unlock(key, lockValue);
        }
    }


}
