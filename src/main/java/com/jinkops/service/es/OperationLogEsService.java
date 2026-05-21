package com.jinkops.service.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.jinkops.entity.log.OperationLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日誌 ES 搜尋服務
 *
 * 功能說明：
 * 1. 透過 ES 進行查詢
 * 2. 回傳列表交給 Controller
 * 3. 例外交給上層處理
 */
@Service
@RequiredArgsConstructor
public class OperationLogEsService {

    private static final String INDEX_NAME = "operation_log_search";
    private final ElasticsearchClient elasticsearchClient;

    public Page<OperationLogEntity> search(
            String keyword,
            Long startTime,
            Long endTime,
            int page,
            int size
    ) throws Exception {
        int from = Math.max(page, 0) * Math.max(size, 1);

        List<Query> mustQueries = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            String text = keyword.trim();
            mustQueries.add(Query.of(q -> q
                    .multiMatch(m -> m
                            .fields(
                                    "username",
                                    "operation",
                                    "description",
                                    "args",
                                    "traceId",
                                    "className",
                                    "methodName",
                                    "uri",
                                    "httpMethod",
                                    "ip"
                            )
                            .query(text)
                            .lenient(true)
                    )
            ));
        }

        if (startTime != null) {
            mustQueries.add(Query.of(q -> q
                    .range(r -> r.untyped(u -> u
                            .field("createTime")
                            .gte(JsonData.of(formatTime(startTime)))
                    ))
            ));
        }

        if (endTime != null) {
            mustQueries.add(Query.of(q -> q
                    .range(r -> r.untyped(u -> u
                            .field("createTime")
                            .lte(JsonData.of(formatTime(endTime)))
                    ))
            ));
        }

        int responseSize = Math.max(size, 1);
        SearchResponse<Map> response = elasticsearchClient.search(s -> s
                        .index(INDEX_NAME)
                        .query(q -> q.bool(BoolQuery.of(b -> b.must(mustQueries))))
                        .sort(so -> so
                                .field(f -> f
                                        .field("createTime")
                                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                                )
                        )
                        .from(from)
                        .size(responseSize),
                Map.class
        );

        List<OperationLogEntity> result = parseResponse(response.hits().hits());
        long total = response.hits().total() != null ? response.hits().total().value() : result.size();

        return new PageImpl<>(result, PageRequest.of(page, responseSize), total);
    }

    public void bulkIndex(List<OperationLogEntity> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (OperationLogEntity entity : entities) {
            String docId = entity.getId() != null ? String.valueOf(entity.getId()) : entity.getTraceId();
            bulkBuilder.operations(op -> op
                    .index(i -> i
                            .index(INDEX_NAME)
                            .id(docId)
                            .document(entityToDoc(entity))
                    )
            );
        }

        BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());
        if (response.errors()) {
            // 只記錄警告，不影響主要查詢流程
            System.err.println("[ES] bulk index returned errors for operation logs");
        }
    }

    private List<OperationLogEntity> parseResponse(List<Hit<Map>> hits) {
        List<OperationLogEntity> result = new ArrayList<>();
        for (Hit<Map> hit : hits) {
            Map source = hit.source();
            if (source == null) {
                continue;
            }
            OperationLogEntity entity = new OperationLogEntity();
            entity.setUsername((String) source.get("username"));
            entity.setOperation((String) source.get("operation"));
            entity.setTraceId((String) source.get("traceId"));
            entity.setClassName((String) source.get("className"));
            entity.setMethodName((String) source.get("methodName"));
            entity.setArgs((String) source.get("args"));
            entity.setDescription((String) source.get("description"));

            Object elapsed = source.get("elapsedTime");
            if (elapsed instanceof Number) {
                entity.setElapsedTime(((Number) elapsed).longValue());
            }
            entity.setCreateTime(parseCreateTime(source.get("createTime")));
            result.add(entity);
        }
        return result;
    }

    private Map<String, Object> entityToDoc(OperationLogEntity entity) {
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
        doc.put("uri", entity.getUri());
        doc.put("httpMethod", entity.getHttpMethod());
        doc.put("ip", entity.getIp());
        return doc;
    }

    private String formatTime(Long epochMilli) {
        return Instant.ofEpochMilli(epochMilli)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private LocalDateTime parseCreateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Instant.ofEpochMilli(((Number) value).longValue())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        if (value instanceof String s) {
            try {
                return OffsetDateTime.parse(s).toLocalDateTime();
            } catch (Exception ignored) {
                try {
                    return LocalDateTime.parse(s);
                } catch (Exception ignored2) {
                    return null;
                }
            }
        }
        return null;
    }
}
