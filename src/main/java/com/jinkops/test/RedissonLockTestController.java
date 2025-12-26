package com.jinkops.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test/redisson")
@RequiredArgsConstructor
public class RedissonLockTestController {

    private final RedissonClient redissonClient;

    @GetMapping("/lock")
    public String testLock() throws InterruptedException {

        RLock lock = redissonClient.getFairLock("lock:redisson:fair");

        log.info("嘗試取得鎖...");
        lock.lock(); // 不給 TTL，Watchdog 生效

        try {
            log.info("鎖已取得，開始執行業務");
            Thread.sleep(10_000);
            log.info("業務執行完成");
        } finally {
            lock.unlock();
            log.info("鎖已釋放");
        }

        return "ok";
    }
    @GetMapping("/read")
    public String read() throws InterruptedException {
        var rw = redissonClient.getReadWriteLock("lock:redisson:rw");
        RLock r = rw.readLock();

        r.lock();
        try {
            log.info("讀鎖進來");
            Thread.sleep(10_000);
        } finally {
            r.unlock();
            log.info("讀鎖出去");
        }
        return "read ok";
    }

    @GetMapping("/write")
    public String write() throws InterruptedException {
        var rw = redissonClient.getReadWriteLock("lock:redisson:rw");
        RLock w = rw.writeLock();

        w.lock();
        try {
            log.info("寫鎖進來");
            Thread.sleep(10_000);
        } finally {
            w.unlock();
            log.info("寫鎖出去");
        }
        return "write ok";
    }

}
