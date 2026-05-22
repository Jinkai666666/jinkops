package com.jinkops.quartz.job;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.jinkops.mq.listener.OperationLogListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 守護 ES 和 MQ 消費者。
 *
 * ES 掛掉時停掉操作日誌消費者，消息留在 MQ 主隊列。
 * ES 恢復後再啟動消費者，慢慢把積壓消息寫回 ES。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScanFailedOperationLogJob implements Job {
    private final ElasticsearchClient elasticsearchClient;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @Override
    public void execute(JobExecutionContext context) {
        MessageListenerContainer container =
                rabbitListenerEndpointRegistry.getListenerContainer(OperationLogListener.LISTENER_ID);
        if (container == null) {
            log.warn("[Quartz] operation log listener container not found");
            return;
        }

        boolean esUp = isEsUp();
        if (!esUp && container.isRunning()) {
            container.stop();
            log.warn("[Quartz] ES down, pause operation log consumer");
            return;
        }

        if (esUp && !container.isRunning()) {
            container.start();
            log.info("[Quartz] ES up, resume operation log consumer");
            return;
        }

        log.info("[Quartz] ES guard checked esUp={} consumerRunning={}", esUp, container.isRunning());
    }

    private boolean isEsUp() {
        try {
            return elasticsearchClient.ping().value();
        } catch (Exception e) {
            return false;
        }
    }
}
