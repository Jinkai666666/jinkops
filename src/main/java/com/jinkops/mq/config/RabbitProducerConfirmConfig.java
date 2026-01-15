package com.jinkops.mq.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RabbitProducerConfirmConfig {

    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {

        // Confirm：是否到達 Exchange
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("【Confirm OK】訊息已到達 Exchange");
            } else {
                log.error("【Confirm FAIL】訊息未到達 Exchange，原因={}", cause);
            }
        });

        // Return：Exchange 有，但路由不到 Queue
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error(
                    "【Return】路由失敗 exchange={} routingKey={} replyText={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyText()
            );
        });

        // 必須，否則 Return 不觸發
        rabbitTemplate.setMandatory(true);
    }
}
