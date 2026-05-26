package com.example.integration.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sanitizes AI payloads for logs/DB: replaces base64 blobs with length hints, never logs API keys.
 */
public final class AiPayloadLogSupport {

    private static final int PREVIEW_CHARS = 120;
    private static final List<String> BASE64_KEYS = List.of(
        "personImageBase64",
        "garmentImageBase64",
        "imageBase64",
        "image",
        "audio",
        "file",
        "data"
    );

    private AiPayloadLogSupport() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> sanitize(Map<String, Object> payload) {
        if (payload == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            out.put(entry.getKey(), sanitizeValue(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object sanitizeValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (isBase64Field(key) && value instanceof String str) {
            return base64Hint(str);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String nestedKey = String.valueOf(e.getKey());
                nested.put(nestedKey, sanitizeValue(nestedKey, e.getValue()));
            }
            return nested;
        }
        if (value instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : list) {
                sanitized.add(item instanceof Map<?, ?> m
                    ? sanitize(castMap(m))
                    : item instanceof String s && looksLikeBase64(s) ? base64Hint(s) : item);
            }
            return sanitized;
        }
        if (value instanceof String str && looksLikeBase64(str)) {
            return base64Hint(str);
        }
        return value;
    }

    private static boolean isBase64Field(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        for (String candidate : BASE64_KEYS) {
            if (lower.contains(candidate.toLowerCase())) {
                return true;
            }
        }
        return lower.contains("base64");
    }

    private static boolean looksLikeBase64(String value) {
        if (value == null || value.length() < 256) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("data:image/") && trimmed.length() > 400) {
            return true;
        }
        return trimmed.length() > 2000 && trimmed.matches("^[A-Za-z0-9+/=\\s]+$");
    }

    private static String base64Hint(String value) {
        int len = value != null ? value.length() : 0;
        String preview = "";
        if (value != null && value.startsWith("data:image/")) {
            int comma = value.indexOf(',');
            preview = comma > 0 ? value.substring(0, Math.min(comma + 1, PREVIEW_CHARS)) + "…" : "data-uri";
        }
        return "[base64 omitted, chars=" + len + (preview.isEmpty() ? "" : ", prefix=" + preview) + "]";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            result.put(String.valueOf(e.getKey()), e.getValue());
        }
        return result;
    }
}
