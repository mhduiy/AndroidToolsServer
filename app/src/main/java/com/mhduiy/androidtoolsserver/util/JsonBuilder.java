package com.mhduiy.androidtoolsserver.util;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON构建器工具类，用于构建JSON字符串
 */
public class JsonBuilder {
    private final Map<String, Object> data = new HashMap<>();

    public JsonBuilder put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public JsonBuilder putRaw(String key, String jsonValue) {
        // 用于直接插入已经是JSON格式的字符串
        data.put(key, new RawJson(jsonValue));
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            appendValue(sb, entry.getValue());
        }

        sb.append("}");
        return sb.toString();
    }

    public Object buildObject() {
        return new HashMap<>(data);
    }

    private void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof RawJson) {
            sb.append(((RawJson) value).json);
        } else if (value instanceof String) {
            sb.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                appendValue(sb, entry.getValue());
            }
            sb.append("}");
        } else {
            // Default to string representation
            sb.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";

        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private static class RawJson {
        final String json;

        RawJson(String json) {
            this.json = json;
        }
    }
}
