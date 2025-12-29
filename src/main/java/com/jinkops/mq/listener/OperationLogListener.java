package com.jinkops.mq.listener;

import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OperationLogListener {

    @RabbitListener(queues = "ops.event.log.queue")
    public void onMessage(OperationLogEntity entity) {
        log.info("MQ 收到操作日志：{}", entity);
    }

}
