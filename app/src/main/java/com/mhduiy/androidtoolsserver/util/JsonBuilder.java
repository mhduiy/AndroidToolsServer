package com.mhduiy.androidtoolsserver.util;

import java.util.Map;
import java.util.List;

public class JsonBuilder {
    private StringBuilder sb = new StringBuilder();

    public JsonBuilder() {
        sb.append("{");
    }

    public JsonBuilder add(String key, String value) {
        addComma();
        sb.append("\"").append(key).append("\":\"").append(escapeJson(value)).append("\"");
        return this;
    }

    public JsonBuilder add(String key, int value) {
        addComma();
        sb.append("\"").append(key).append("\":").append(value);
        return this;
    }

    public JsonBuilder add(String key, long value) {
        addComma();
        sb.append("\"").append(key).append("\":").append(value);
        return this;
    }

    public JsonBuilder add(String key, double value) {
        addComma();
        sb.append("\"").append(key).append("\":").append(value);
        return this;
    }

    public JsonBuilder add(String key, boolean value) {
        addComma();
        sb.append("\"").append(key).append("\":").append(value);
        return this;
    }

    public JsonBuilder add(String key, String value, boolean escapeValue) {
        addComma();
        sb.append("\"").append(key).append("\":");
        if (escapeValue) {
            sb.append("\"").append(escapeJson(value)).append("\"");
        } else {
            sb.append(value); // 直接添加原始JSON字符串
        }
        return this;
    }

    public JsonBuilder add(String key, List<?> list) {
        addComma();
        sb.append("\"").append(key).append("\":[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            Object item = list.get(i);
            if (item instanceof String) {
                sb.append("\"").append(escapeJson((String) item)).append("\"");
            } else {
                sb.append(item);
            }
        }
        sb.append("]");
        return this;
    }

    public JsonBuilder add(String key, Map<String, Object> map) {
        addComma();
        sb.append("\"").append(key).append("\":");
        sb.append(mapToJson(map));
        return this;
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder mapSb = new StringBuilder();
        mapSb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) mapSb.append(",");
            first = false;
            mapSb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                mapSb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Map) {
                mapSb.append(mapToJson((Map<String, Object>) value));
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                mapSb.append("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) mapSb.append(",");
                    Object item = list.get(i);
                    if (item instanceof String) {
                        mapSb.append("\"").append(escapeJson((String) item)).append("\"");
                    } else {
                        mapSb.append(item);
                    }
                }
                mapSb.append("]");
            } else {
                mapSb.append(value);
            }
        }
        mapSb.append("}");
        return mapSb.toString();
    }

    private void addComma() {
        if (sb.length() > 1) {
            sb.append(",");
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public String build() {
        sb.append("}");
        return sb.toString();
    }

    public static String fromMap(Map<String, Object> map) {
        JsonBuilder builder = new JsonBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                builder.add(entry.getKey(), (String) value);
            } else if (value instanceof Integer) {
                builder.add(entry.getKey(), (Integer) value);
            } else if (value instanceof Long) {
                builder.add(entry.getKey(), (Long) value);
            } else if (value instanceof Double) {
                builder.add(entry.getKey(), (Double) value);
            } else if (value instanceof Boolean) {
                builder.add(entry.getKey(), (Boolean) value);
            } else if (value instanceof List) {
                builder.add(entry.getKey(), (List<?>) value);
            } else if (value instanceof Map) {
                builder.add(entry.getKey(), (Map<String, Object>) value);
            }
        }
        return builder.build();
    }
}
