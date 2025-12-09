package com.jinkops.test;

import com.jinkops.lock.RedisLock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestLockController {

    private final RedisLock redisLock;

    public TestLockController(RedisLock redisLock) {
        this.redisLock = redisLock;
    }

    @GetMapping("/api/test/lock")
    public String testLock() throws InterruptedException {

        String key = "lock:test";
        String value = redisLock.tryLock(key, 5);

        if (value == null) {
            return "未获取到锁";
        }

        System.out.println("进入业务代码区");

        // 模拟业务执行
        Thread.sleep(3000);

        redisLock.unlock(key, value);

        return "执行完成";
    }
}
