package com.jinkops.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // 入口
    public static final String EVENT_EXCHANGE = "ops.event.exchange";

    // 存消息的队列
    public static final String EVENT_LOG_QUEUE = "ops.event.log.queue";

    // 路由用的 key
    public static final String EVENT_LOG_KEY = "ops.event.log";

    @Bean
    public DirectExchange eventExchange() {
        // direct 类型，重启不丢
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue eventLogQueue() {
        // 普通持久化队列
        return QueueBuilder.durable(EVENT_LOG_QUEUE).build();
    }
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Binding eventLogBinding() {
        // 用 routingKey 把 exchange 和 queue 绑起来
        return BindingBuilder
                .bind(eventLogQueue())
                .to(eventExchange())
                .with(EVENT_LOG_KEY);
    }
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
