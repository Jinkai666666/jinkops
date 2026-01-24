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
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogListener {

    private static final int MAX_RETRY = 3;
    private static final String INDEX_NAME = "operation_log_search";

    private final ElasticsearchClient elasticsearchClient;

    @PostConstruct
    public void ensureIndex() {
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
            // 先寫 ES，再決定 ACK / 重試
            indexToEs(entity);

            channel.basicAck(tag, false);
            log.info("[MQ] consume success retry={} dlq=false source=ES", retryCount);

        } catch (Exception e) {

            if (retryCount >= MAX_RETRY) {
                log.error("[MQ] consume failed retry={} dlq=true reason={}", retryCount, e.getMessage(), e);
                channel.basicNack(tag, false, false);

            } else {
                int nextRetry = retryCount + 1;

                message.getMessageProperties()
                        .getHeaders()
                        .put("x-retry-count", nextRetry);

                log.error("[MQ] consume failed retry={} dlq=false reason={}", nextRetry, e.getMessage(), e);
                channel.basicNack(tag, false, true);
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
        doc.put("username", entity.getUsername());
        doc.put("operation", entity.getOperation());
        doc.put("traceId", entity.getTraceId());
        doc.put("className", entity.getClassName());
        doc.put("methodName", entity.getMethodName());
        doc.put("args", entity.getArgs());
        doc.put("description", entity.getDescription());
        doc.put("elapsedTime", entity.getElapsedTime());
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
