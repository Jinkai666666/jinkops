package com.jinkops.mq.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    // 死信 Exchange
    public static final String DLX_EXCHANGE = "ops.event.dlx.exchange";
    // 死信 Queue
    public static final String DLQ_QUEUE = "ops.event.dlq.queue";
    // 死信 routingKey
    public static final String DLQ_KEY = "ops.event.dlq";

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(dlqQueue())
                .to(dlxExchange())
                .with(DLQ_KEY);
    }

    // 入口
    public static final String EVENT_EXCHANGE = "ops.event.exchange";

    // 存訊息的佇列
    public static final String EVENT_LOG_QUEUE = "ops.event.log.queue";

    // 路由用的 key
    public static final String EVENT_LOG_KEY = "ops.event.log";

    @Bean
    public DirectExchange eventExchange() {
        // direct 型別，重啟不丟
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    // 佇列
    public Queue eventLogQueue() {
        return QueueBuilder.durable(EVENT_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_KEY)
                .build();
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Binding eventLogBinding() {
        // 用 routingKey 把 exchange 和 queue 綁起來
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
