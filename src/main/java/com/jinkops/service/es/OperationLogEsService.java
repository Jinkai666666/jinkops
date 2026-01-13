package com.jinkops.service.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.jinkops.entity.log.OperationLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ???????????? ES ????????????
 *
 * ?????????
 * 1. ???????????? Elasticsearch
 * 2. ???????????????????????????Controller???
 * 3. ?????????????????????Exception
 */
@Service
@RequiredArgsConstructor
public class OperationLogEsService {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * ?????????????????????ES???
     *
     * @param keyword   ????????????username / operation???
     * @param startTime ???????????????epoch milli???
     * @param endTime   ???????????????epoch milli???
     */
    public List<OperationLogEntity> search(
            String keyword,
            Long startTime,
            Long endTime
    ) throws Exception {

        List<Query> mustQueries = new ArrayList<>();

        // ===== ??????????????????username / operation???====
        if (keyword != null && !keyword.isBlank()) {
            mustQueries.add(Query.of(q -> q
                    .multiMatch(m -> m
                            .fields("username", "operation")
                            .query(keyword)
                    )
            ));
        }

        // ===== ?????????????????????createTime???====
        if (startTime != null || endTime != null) {

            mustQueries.add(Query.of(q -> q
                    .range(r -> r.untyped(u -> {
                        u.field("createTime");

                        if (startTime != null) {
                            u.gte(JsonData.of(formatTime(startTime)));
                        }
                        if (endTime != null) {
                            u.lte(JsonData.of(formatTime(endTime)));
                        }
                        return u;
                    }))
            ));
        }

        // ===== ?????? Bool Query =====
        BoolQuery boolQuery = BoolQuery.of(b -> b.must(mustQueries));

        // ===== ???????????? =====
        SearchResponse<Map> response =
                elasticsearchClient.search(s -> s
                                .index("operation_log_search")
                                .query(q -> q.bool(boolQuery))
                                .sort(so -> so
                                        .field(f -> f
                                                .field("createTime")
                                                .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                                        )
                                )
                                .size(100), // ????????????????????????
                        Map.class
                );

        // ===== ?????????=====
        List<OperationLogEntity> result = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
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

    /**
     * epoch milli ???ES ??????????????????
     */
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
