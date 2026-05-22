package com.jinkops.mq.listener;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.mq.config.RabbitConfig;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogListener {

    public static final String LISTENER_ID = "operationLogListener";
    private static final String INDEX_NAME = "operation_log_search";

    private final ElasticsearchClient elasticsearchClient;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @PostConstruct
    public void ensureIndex() {
        log.info("[MQ] operation log listener ready queue={}", RabbitConfig.EVENT_LOG_QUEUE);
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(e -> e.index(INDEX_NAME))
                    .value();
            if (!exists) {
                elasticsearchClient.indices().create(c -> c
                        .index(INDEX_NAME)
                        .mappings(m -> m
                                .properties("username", p -> p.keyword(k -> k))
                                .properties("operation", p -> p.text(t -> t))
                                .properties("traceId", p -> p.keyword(k -> k))
                                .properties("className", p -> p.keyword(k -> k))
                                .properties("methodName", p -> p.keyword(k -> k))
                                .properties("args", p -> p.text(t -> t))
                                .properties("description", p -> p.text(t -> t))
                                .properties("elapsedTime", p -> p.long_(l -> l))
                                .properties("createTime", p -> p.date(d -> d))
                                .properties("uri", p -> p.keyword(k -> k))
                                .properties("httpMethod", p -> p.keyword(k -> k))
                                .properties("ip", p -> p.keyword(k -> k))
                        ));
                log.info("[MQ] created index {}", INDEX_NAME);
            }
        } catch (Exception e) {
            log.warn("[MQ] failed to ensure index {}: {}", INDEX_NAME, e.getMessage());
        }
    }

    @RabbitListener(
            id = LISTENER_ID,
            queues = RabbitConfig.EVENT_LOG_QUEUE,
            ackMode = "MANUAL",
            autoStartup = "true"
    )
    public void onMessage(
            OperationLogEntity entity,
            Message message,
            Channel channel
    ) throws Exception {

        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            // 先寫 ES，再決定 ACK / 重試
            indexToEs(entity);

            channel.basicAck(tag, false);
            log.info("[MQ] consume success dlq=false source=ES");

        } catch (Exception e) {
            // ES 掛掉就先放回主隊列，然後暫停消費者，等 Quartz 偵測恢復後再繼續。
            log.error("[MQ] consume failed requeue=true pauseConsumer=true reason={}", e.getMessage(), e);
            channel.basicNack(tag, false, true);
            MessageListenerContainer container =
                    rabbitListenerEndpointRegistry.getListenerContainer(LISTENER_ID);
            if (container != null && container.isRunning()) {
                container.stop();
                log.warn("[MQ] paused operation log consumer because ES write failed");
            }
        }
    }

    private void indexToEs(OperationLogEntity entity) throws Exception {
        if (entity == null) {
            throw new IllegalArgumentException("operation log entity is null");
        }

        String docId = entity.getId() != null
                ? String.valueOf(entity.getId())
                : entity.getTraceId();

        Map<String, Object> doc = new HashMap<>();
        doc.put("id", entity.getId());
        doc.put("username", entity.getUsername());
        doc.put("operation", entity.getOperation());
        doc.put("traceId", entity.getTraceId());
        doc.put("className", entity.getClassName());
        doc.put("methodName", entity.getMethodName());
        doc.put("args", entity.getArgs());
        doc.put("description", entity.getDescription());
        doc.put("elapsedTime", entity.getElapsedTime());
        doc.put("uri", entity.getUri());
        doc.put("httpMethod", entity.getHttpMethod());
        doc.put("ip", entity.getIp());
        if (entity.getCreateTime() != null) {
            long epochMillis = entity.getCreateTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            doc.put("createTime", epochMillis);
        }

        IndexResponse response = elasticsearchClient.index(i -> i
                        .index(INDEX_NAME)
                        .id(docId)
                        .document(doc)
        );
        log.info("[MQ] indexed operation log to ES id={} result={}", docId, response.result());
    }
}
