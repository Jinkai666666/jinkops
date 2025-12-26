package com.jinkops.test;
import com.jinkops.service.EventLogService;
import com.jinkops.lock.RedisLock;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestLockController {

    private final RedisLock redisLock;
    private  EventLogService eventLogService;

    public TestLockController(RedisLock redisLock,
                              EventLogService eventLogService) {
        this.redisLock = redisLock;
        this.eventLogService = eventLogService;
    }

    @GetMapping("/api/test/lock")
    public String testLock() throws InterruptedException {

        String key = "lock:test";
        String value = redisLock.tryLock(key, 5);

        if (value == null) {
            return "未獲取到鎖";
        }

        System.out.println("進入業務代碼區");

        // 模擬業務執行
        Thread.sleep(3000);

        redisLock.unlock(key, value);

        return "執行完成";
    }
    @GetMapping("/test/mq")
    public String testMq() {
        eventLogService.sendTestLog();
        return "ok";
    }

}
