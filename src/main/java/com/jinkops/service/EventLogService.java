package com.jinkops.service;

import com.jinkops.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventLogService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendTestLog() {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EVENT_EXCHANGE, // 发到哪个 exchange
                RabbitConfig.EVENT_LOG_KEY,  // 用哪个 routingKey
                "hello rabbitmq"             // 消息内容
        );
    }
}
