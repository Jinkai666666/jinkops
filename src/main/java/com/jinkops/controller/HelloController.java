package com.jinkops.controller;

import com.jinkops.annotation.OperationLog;
import com.jinkops.service.GreetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//
@RestController
public class HelloController {

    private final GreetingService greetingService;

    @Autowired
    public HelloController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    //测试接口http://localhost:8080/hello?name=
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "World") String name) {
        return greetingService.greeting(name);
    }

    @OperationLog("测试日志入库")
    @GetMapping("/test/log")
    public String testLog() {
        return "OK";
    }

    @OperationLog("测试异常入库")
    @GetMapping("/test/error")
    public String testError() {
        throw new RuntimeException("这是个测试异常");
    }
}
