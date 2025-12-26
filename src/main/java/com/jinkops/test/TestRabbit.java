package com.jinkops.test;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TestRabbit {

    @RabbitListener(queues = "ops.event.log.queue")
    public void onMessage(String msg) {
        System.out.println("收到消息：" + msg);
    }
}
