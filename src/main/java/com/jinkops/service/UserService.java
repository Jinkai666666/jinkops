package com.jinkops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinkops.cache.key.UserKeys;
import com.jinkops.cache.service.CacheService;
import com.jinkops.cache.service.PermissionCache;
import com.jinkops.entity.user.Role;
import com.jinkops.entity.user.User;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.repository.RoleRepository;
import com.jinkops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedissonClient redissonClient;
    private final PermissionCache permissionCache;
    private final RoleRepository roleRepository;

    // 隨機 TTL，避免大量快取同時過期
    private int randomTtl() {
        return 3600 + (int) (Math.random() * 300); // 隨機 3600~3900
    }

    // 注入 repository
    // 後台列表入口，直接走資料庫
    public List<User> getAllUsers() {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] getAllUsers start keyParams=none");
        try {
            List<User> result = userRepository.findAll();
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] getAllUsers success cost={}ms keyResult=count={}", cost, result.size());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] getAllUsers failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 查用戶資訊，走快取避免打穿 DB
    public User findByUsername(String username) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] findByUsername start keyParams=username={}", username);
        try {
            String key = UserKeys.userInfo(username);
            String json = cacheService.get(key);
            if ("null".equals(json)) {
                // 空值快取命中，直接回
                long cost = System.currentTimeMillis() - start;
                log.info("[SERVICE] findByUsername success cost={}ms keyResult=not_found", cost);
                return null;
            }

            if (json != null) {
                try {
                    // 快取命中直接反序列化
                    User cached = objectMapper.readValue(json, User.class);
                    long cost = System.currentTimeMillis() - start;
                    log.info("[SERVICE] findByUsername success cost={}ms keyResult=from_cache", cost);
                    return cached;
                } catch (Exception ignored) {
                }
            }

            // 快取沒有就回 DB
            User user = userRepository.findByUsername(username);
            if (user == null) {
                // 空值快取防穿透
                cacheService.set(key, "null", 30);
                long cost = System.currentTimeMillis() - start;
                log.info("[SERVICE] findByUsername success cost={}ms keyResult=not_found", cost);
                return null;
            }

            try {
                // 有結果就補快取
                cacheService.set(key, objectMapper.writeValueAsString(user), randomTtl());
            } catch (Exception ignored) {
            }
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] findByUsername success cost={}ms keyResult=found", cost);
            return user;
        } catch (Exception e) {
            log.error("[SERVICE] findByUsername failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 新增用戶時做密碼加密
    @Transactional
    public User addUser(User user) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] addUser start keyParams=username={}", user.getUsername());
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            Role defaultRole = roleRepository.findByCodeIgnoreCase("USER");
            if (defaultRole != null) {
                user.setRoles(Set.of(defaultRole));
            }
            User saved = userRepository.save(user);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] addUser success cost={}ms keyResult=userId={}", cost, saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("[SERVICE] addUser failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 刪除後順便清理分頁快取
    @Transactional
    public void deleteUser(String username) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] deleteUser start keyParams=username={}", username);
        try {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                // 找不到就直接丟業務錯
                throw new BizException(ErrorCode.USER_NOT_FOUND, "用戶不存在：" + username);
            }
            cacheService.deleteByPrefix("user:list:page:");
            userRepository.delete(user);
            permissionCache.delete(username);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] deleteUser success cost={}ms keyResult=ok", cost);
        } catch (Exception e) {
            log.error("[SERVICE] deleteUser failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 內部方法，給授權/關聯關係查詢用
    public Optional<User> getById(Long id) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] getById start keyParams=id={}", id);
        try {
            Optional<User> result = userRepository.findById(id);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] getById success cost={}ms keyResult=found={}", cost, result.isPresent());
            return result;
        } catch (Exception e) {
            log.error("[SERVICE] getById failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 只讀快取，不做 DB 回查
    public User getUserFromCache(String username) {
        String key = UserKeys.userInfo(username);
        String json = cacheService.get(key);
        if (json == null) {
            return null;
        }
        if ("null".equals(json)) {
            return null; // 空值快取
        }
        try {
            return objectMapper.readValue(json, User.class);
        } catch (Exception e) {
            return null;
        }
    }

    // 統一快取寫入入口，方便控 TTL
    public void setUserCache(String username, User user) {
        try {
            String key = UserKeys.userInfo(username);
            String json = objectMapper.writeValueAsString(user);
            cacheService.set(key, json, randomTtl());
        } catch (Exception ignored) {
        }
    }

    // 更新後要清理對應快取與分頁快取
    @Transactional
    public User updateUser(User user) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] updateUser start keyParams=username={}", user.getUsername());
        try {
            User dbUser = userRepository.findByUsername(user.getUsername());
            if (dbUser == null) {
                // 更新前先確定資料存在
                throw new BizException(ErrorCode.USER_NOT_FOUND, "用戶不存在：" + user.getUsername());
            }
            dbUser.setEmail(user.getEmail());
            dbUser.setPassword(passwordEncoder.encode(user.getPassword()));

            User updated = userRepository.save(dbUser);

            String key = UserKeys.userInfo(updated.getUsername());
            cacheService.delete(key);
            cacheService.deleteByPrefix("user:list:page:");

            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] updateUser success cost={}ms keyResult=userId={}", cost, updated.getId());
            return updated;
        } catch (Exception e) {
            log.error("[SERVICE] updateUser failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 分頁列表走快取，降低列表壓力
    public Page<User> pageUsers(int page, int size) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] pageUsers start keyParams=page={},size={}", page, size);
        try {
            String key = UserKeys.userListPage(page, size);
            String json = cacheService.get(key);

            if (json != null) {
                try {
                    CachedPage cached = objectMapper.readValue(json, CachedPage.class);
                    Page<User> result = new PageImpl<>(
                            cached.getContent(),
                            PageRequest.of(page - 1, size),
                            cached.getTotal()
                    );
                    long cost = System.currentTimeMillis() - start;
                    log.info("[SERVICE] pageUsers success cost={}ms keyResult=total={}",
                            cost, result.getTotalElements());
                    return result;
                } catch (Exception ignored) {
                }
            }

            PageRequest pr = PageRequest.of(page - 1, size);
            Page<User> pageData = userRepository.findAll(pr);

            try {
                CachedPage cached = new CachedPage();
                cached.setContent(pageData.getContent());
                cached.setTotal(pageData.getTotalElements());
                String listJson = objectMapper.writeValueAsString(cached);
                cacheService.set(key, listJson, randomTtl());
            } catch (Exception ignored) {
            }

            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] pageUsers success cost={}ms keyResult=total={}",
                    cost, pageData.getTotalElements());
            return pageData;
        } catch (Exception e) {
            log.error("[SERVICE] pageUsers failed reason={}", e.getMessage(), e);
            throw e;
        }
    }

    // 這裡用分散式鎖，避免併發建立同名用戶
    @Transactional
    public User createUser(User user) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] createUser start keyParams=username={}", user.getUsername());
        String username = user.getUsername();
        String lockKey = "lock:user:create:" + username;

        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(5, 15, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(ErrorCode.REPEAT_SUBMIT, "請稍後重試");
            }

            User exist = userRepository.findByUsername(username);
            if (exist != null) {
                // 同名就直接拒絕
                throw new BizException(ErrorCode.USER_EXIST, "用戶名已存在");
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User saved = userRepository.save(user);

            cacheService.deleteByPrefix("user:list:page:");
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] createUser success cost={}ms keyResult=userId={}", cost, saved.getId());
            return saved;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[SERVICE] createUser interrupted reason={}", e.getMessage(), e);
            throw new BizException(ErrorCode.REPEAT_SUBMIT, "請稍後重試");
        } catch (Exception e) {
            log.error("[SERVICE] createUser failed reason={}", e.getMessage(), e);
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                // 只解自己持有的鎖
                lock.unlock();
            }
        }
    }

    // 用於序列化分頁快取
    private static class CachedPage {
        private List<User> content;
        private long total;

        public List<User> getContent() {
            return content;
        }

        public void setContent(List<User> content) {
            this.content = content;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }
    }
}
