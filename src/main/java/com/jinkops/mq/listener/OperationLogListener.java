package com.jinkops.mq.listener;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.mq.config.RabbitConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Component
@Slf4j
public class OperationLogListener {

    private static final int MAX_RETRY = 3;

    @RabbitListener(
            queues = RabbitConfig.EVENT_LOG_QUEUE,
            ackMode = "MANUAL"
    )
    public void onMessage(
            OperationLogEntity entity,
            Message message,
            Channel channel
    ) throws Exception {

        long tag = message.getMessageProperties().getDeliveryTag();

        // 从 header 里拿重试次数
        Integer retryCount = (Integer) message
                .getMessageProperties()
                .getHeaders()
                .getOrDefault("x-retry-count", 0);

        try {
            // ===== 业务处理 =====
            log.info("MQ 收到操作日志：{}", entity);

            //测试用：先故意抛异常
            // int x = 1 / 0;

            // 成功才 ACK
            channel.basicAck(tag, false);

        } catch (Exception e) {

            if (retryCount >= MAX_RETRY) {
                log.error(
                        "超过最大重试次数，进入死信队列，retryCount={}, entity={}",
                        retryCount, entity, e
                );

                // requeue=false → 触发 DLQ
                channel.basicNack(tag, false, false);

            } else {
                int nextRetry = retryCount + 1;

                // 更新 header
                message.getMessageProperties()
                        .getHeaders()
                        .put("x-retry-count", nextRetry);

                log.warn(
                        "消费失败，准备第 {} 次重试，entity={}",
                        nextRetry, entity, e
                );

                // 继续重试
                channel.basicNack(tag, false, true);
            }
        }
    }
}
