package com.jinkops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinkops.cache.key.UserKeys;
import com.jinkops.cache.service.CacheService;
import com.jinkops.entity.user.User;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

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
        return userRepository.findByUsername(username);
    }

    //  新增用户 (Create new user)
    public User addUser(User user) {
        return userRepository.save(user);
    }
    //  删除用户 (Delete by username)
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在：" + username);
        }

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
        }try{
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
            cacheService.set(key,json,3600);
        }catch (Exception ignored){}
    }

}
