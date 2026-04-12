package com.example.integration.tts;

import java.util.List;
import java.util.Map;

/**
 * Достаёт текст ответа ассистента из OpenAI-совместимого JSON (chat/completions).
 */
public final class ChatAssistantTextExtractor {

    private ChatAssistantTextExtractor() {}

    @SuppressWarnings("unchecked")
    public static String extractAssistantContent(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object msgObj = choice.get("message");
        if (!(msgObj instanceof Map<?, ?> message)) {
            return null;
        }
        Object content = message.get("content");
        if (content == null) {
            return null;
        }
        return content.toString();
    }
}
