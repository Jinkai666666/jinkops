package com.jinkops;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JinkopsApplicationTests {

    @Test
    void contextLoads() {
        // 禁止加载数据库或完整上下文
        System.out.println("Test ran successfully.");
    }
}
