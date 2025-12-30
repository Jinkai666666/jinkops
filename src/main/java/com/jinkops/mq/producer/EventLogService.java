package com.jinkops.mq.producer;

import com.jinkops.mq.config.RabbitConfig;
import com.jinkops.entity.log.OperationLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventLogService {


    private final RabbitTemplate rabbitTemplate;
     //发送操作日志事件
     public void sendOperationLog(OperationLogEntity entity) {

         CorrelationData correlationData =
                 new CorrelationData(entity.getTraceId());

         rabbitTemplate.convertAndSend(
                 RabbitConfig.EVENT_EXCHANGE,
                 RabbitConfig.EVENT_LOG_KEY,
                 entity,
                 correlationData
         );
     }
}
