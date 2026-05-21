package com.jinkops.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class AuditContext {

    // 用 ThreadLocal 暫存這次請求裡的補充資訊，像 Redis 命中、ES 有沒有走到。
    private static final ThreadLocal<Map<String, String>> VALUES =
            ThreadLocal.withInitial(LinkedHashMap::new);

    private AuditContext() {
    }

    public static void put(String key, Object value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        VALUES.get().put(key, String.valueOf(value));
    }

    // 給操作日誌切面用，把剛剛記下來的資訊整理成一小段文字。
    public static String summary() {
        Map<String, String> values = VALUES.get();
        if (values.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner("; ");
        values.forEach((key, value) -> joiner.add(key + "=" + value));
        return joiner.toString();
    }

    // 一個請求結束就清掉，避免下一個請求拿到舊資料。
    public static void clear() {
        VALUES.remove();
    }
}
