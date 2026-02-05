package com.jinkops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@EnableRabbit
@SpringBootApplication
public class JinkopsApplication {
    public static void main(String[] args) {
        SpringApplication.run(JinkopsApplication.class, args);

        PasswordEncoder encoder = new BCryptPasswordEncoder();

    }
}
