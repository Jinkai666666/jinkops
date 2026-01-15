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

        Integer retryCount = (Integer) message
                .getMessageProperties()
                .getHeaders()
                .getOrDefault("x-retry-count", 0);

        try {
            // 這裡只做落庫/審計後續，不影響主流程

            // 成功才 ACK
            channel.basicAck(tag, false);
            log.info("[MQ] consume success retry={} dlq=false", retryCount);

        } catch (Exception e) {

            if (retryCount >= MAX_RETRY) {
                // 達上限就丟 DLQ
                log.error("[MQ] consume failed retry={} dlq=true reason={}", retryCount, e.getMessage(), e);
                // requeue=false → 觸發 DLQ
                channel.basicNack(tag, false, false);

            } else {
                int nextRetry = retryCount + 1;

                // 更新 header
                message.getMessageProperties()
                        .getHeaders()
                        .put("x-retry-count", nextRetry);

                // 失敗先重試，不阻塞主流程
                log.error("[MQ] consume failed retry={} dlq=false reason={}", nextRetry, e.getMessage(), e);
                // 繼續重試
                channel.basicNack(tag, false, true);
            }
        }
    }
}
