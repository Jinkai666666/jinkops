package com.jinkops.mq.producer;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.mq.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventLogService {

    private final RabbitTemplate rabbitTemplate;

    // 發送操作日誌事件
    public void sendOperationLog(OperationLogEntity entity) {

        try {
            CorrelationData correlationData =
                    new CorrelationData(entity.getTraceId());

            rabbitTemplate.convertAndSend(
                    RabbitConfig.EVENT_EXCHANGE,
                    RabbitConfig.EVENT_LOG_KEY,
                    entity,
                    correlationData
            );
            log.info("[MQ] operation log published traceId={}", entity.getTraceId());
        } catch (Exception e) {
            log.warn("[MQ] operation log publish failed: {}", e.getMessage());
        }
    }
}
