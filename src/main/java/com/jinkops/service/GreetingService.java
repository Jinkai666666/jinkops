package com.jinkops.service;
import org.springframework.stereotype.Service;


//简易接口
@Service
public class GreetingService {
    public String greeting(String name) {
        if (name == null || name.isBlank()) {
            name = "World";

        }
        return "Hello " + name;
    }
}
