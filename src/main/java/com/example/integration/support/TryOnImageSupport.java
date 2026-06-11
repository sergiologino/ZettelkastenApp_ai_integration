package com.example.integration.support;

import java.util.Map;

public final class TryOnImageSupport {

    private TryOnImageSupport() {
    }

    public static String extractString(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value instanceof String str && !str.isBlank() ? str : null;
    }

    public static String toDataUri(String base64, String defaultMime) {
        String trimmed = base64.trim();
        if (trimmed.startsWith("data:image/")) {
            return trimmed;
        }
        String mime = defaultMime != null ? defaultMime : "image/jpeg";
        return "data:" + mime + ";base64," + trimmed;
    }

    public static String toFashnImage(String base64OrUrl) {
        if (base64OrUrl == null || base64OrUrl.isBlank()) {
            return null;
        }
        String trimmed = base64OrUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return toDataUri(trimmed, "image/jpeg");
    }

    /** Kling accepts URL or raw base64 without data: prefix. */
    public static String toKlingImage(String base64OrUrl) {
        if (base64OrUrl == null || base64OrUrl.isBlank()) {
            return null;
        }
        String trimmed = base64OrUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("data:image/")) {
            int comma = trimmed.indexOf(',');
            if (comma > 0 && comma < trimmed.length() - 1) {
                return trimmed.substring(comma + 1).trim();
            }
        }
        return trimmed;
    }
}
