package com.example.integration.support;

import java.util.Map;

public final class TokenUsageExtractor {

    private TokenUsageExtractor() {
    }

    public static Integer extract(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return 0;
        }

        Integer direct = firstInt(response, "tokensUsed", "tokens_used", "totalTokens", "total_tokens");
        if (direct != null && direct > 0) {
            return direct;
        }

        Integer credits = firstInt(response, "creditsUsed", "credits_used", "credits");
        if (credits != null && credits > 0) {
            return credits;
        }

        Object usage = response.get("usage");
        if (usage instanceof Map<?, ?> usageMap) {
            Integer fromUsage = firstInt(usageMap, "total_tokens", "totalTokens", "tokens", "credits");
            if (fromUsage != null && fromUsage > 0) {
                return fromUsage;
            }
        }

        return direct != null ? direct : 0;
    }

    private static Integer firstInt(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            Integer parsed = toInt(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
