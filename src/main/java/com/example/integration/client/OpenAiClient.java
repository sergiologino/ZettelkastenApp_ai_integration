package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Клиент для OpenAI (GPT-4, GPT-3.5, etc.)
 * API: https://platform.openai.com/docs/api-reference
 */
@Component
public class OpenAiClient extends BaseNeuralClient {
    
    public OpenAiClient(RestTemplate restTemplate, ObjectMapper objectMapper, com.example.integration.security.EncryptionService encryptionService) {
        super(restTemplate, objectMapper, encryptionService);
    }
    
    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        Map<String, Object> mappedPayload = applyRequestMapping(payload, network.getRequestMapping());

        // Удаляем служебные поля
        Object settingsRaw = mappedPayload.remove("settings");
        mappedPayload.remove("mode");

        if ("image_generation".equalsIgnoreCase(network.getNetworkType())) {
            return sendImageGenerationRequest(network, mappedPayload, settingsRaw);
        }

        if (settingsRaw instanceof Map<?, ?> settingsMap) {
            Object temperature = settingsMap.get("temperature");
            if (temperature instanceof Number && !mappedPayload.containsKey("temperature")) {
                mappedPayload.put("temperature", ((Number) temperature).doubleValue());
            }
            Object maxTokens = settingsMap.get("maxTokens");
            if (maxTokens instanceof Number && !mappedPayload.containsKey("max_tokens")) {
                mappedPayload.put("max_tokens", ((Number) maxTokens).intValue());
            }
        }

        if (!mappedPayload.containsKey("model")) {
            mappedPayload.put("model", network.getModelName() != null ? network.getModelName() : "gpt-4");
        }

        HttpHeaders headers = prepareHeaders(network);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(mappedPayload, headers);

        String url = ensurePath(network.getApiUrl(), "/chat/completions");
        Objects.requireNonNull(url, "Resolved OpenAI endpoint is null");

        HttpMethod method = Objects.requireNonNull(HttpMethod.POST, "HttpMethod constant must be available");
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            method,
            request,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> responseBody = response.getBody();
        return applyResponseMapping(responseBody, network.getResponseMapping());
    }

    private Map<String, Object> sendImageGenerationRequest(
        NeuralNetwork network,
        Map<String, Object> payload,
        Object settingsRaw
    ) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();

        String prompt = extractPrompt(payload);
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt is required for OpenAI image generation.");
        }
        requestBody.put("prompt", prompt);

        String model = resolveModel(payload, network, "dall-e-3");
        requestBody.put("model", model);

        if (payload.containsKey("n")) {
            requestBody.put("n", payload.get("n"));
        }
        if (payload.containsKey("quality")) {
            requestBody.put("quality", normalizeQuality(payload.get("quality")));
        }
        if (payload.containsKey("style")) {
            requestBody.put("style", payload.get("style"));
        }

        Map<String, Object> settings = extractSettings(settingsRaw);

        String size = deriveImageSize(settings);
        if (size != null) {
            requestBody.put("size", size);
        } else {
            requestBody.put("size", "1024x1024");
        }
        if (settings.containsKey("quality") && !requestBody.containsKey("quality")) {
            requestBody.put("quality", normalizeQuality(settings.get("quality")));
        }
        if (settings.containsKey("style") && !requestBody.containsKey("style")) {
            requestBody.put("style", settings.get("style"));
        }
        if (settings.containsKey("n") && !requestBody.containsKey("n")) {
            requestBody.put("n", settings.get("n"));
        }

        HttpHeaders headers = prepareHeaders(network);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String url = ensurePath(network.getApiUrl(), "/generations");
        Objects.requireNonNull(url, "Resolved OpenAI endpoint is null");

        HttpMethod method = Objects.requireNonNull(HttpMethod.POST, "HttpMethod constant must be available");
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            method,
            request,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> responseBody = response.getBody();
        return applyResponseMapping(responseBody, network.getResponseMapping());
    }

    private String extractPrompt(Map<String, Object> payload) {
        Object prompt = payload.get("prompt");
        if (prompt instanceof String str && !str.isBlank()) {
            return str;
        }
        Object messages = payload.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty()) {
            Object last = list.get(list.size() - 1);
            if (last instanceof Map<?, ?> message) {
                Object content = message.get("content");
                if (content instanceof String str && !str.isBlank()) {
                    return str;
                }
            }
        }
        return null;
    }

    private Map<String, Object> extractSettings(Object settingsRaw) {
        Map<String, Object> settings = new HashMap<>();
        if (settingsRaw instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if (key != null) {
                    settings.put(String.valueOf(key), value);
                }
            });
        }
        return settings;
    }

    private String resolveModel(Map<String, Object> payload, NeuralNetwork network, String fallback) {
        Object payloadModel = payload.get("model");
        if (payloadModel instanceof String str && !str.isBlank()) {
            return str;
        }
        if (network.getModelName() != null && !network.getModelName().isBlank()) {
            return network.getModelName();
        }
        return fallback;
    }

    private String deriveImageSize(Map<String, Object> settings) {
        Integer width = toInt(settings.get("width"));
        Integer height = toInt(settings.get("height"));
        if (width != null && height != null) {
            return normalizeSizeByAspect(width, height);
        }
        Object ratio = settings.get("aspectRatio");
        if (ratio instanceof String str && !str.isBlank()) {
            return switch (str) {
                case "16:9" -> "1792x1024";
                case "9:16" -> "1024x1792";
                default -> "1024x1024";
            };
        }
        return null;
    }

    private Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizeSizeByAspect(int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        double aspect = (double) width / (double) height;
        if (Math.abs(aspect - 1.0) < 0.05) {
            return "1024x1024";
        }
        if (aspect > 1.0) {
            return "1792x1024";
        }
        return "1024x1792";
    }

    /**
     * Нормализует значение quality для OpenAI DALL-E API.
     * Допустимые значения: 'standard', 'hd'.
     * Маппинг: 'high' -> 'hd', 'low' -> 'standard', иначе без изменений.
     */
    private String normalizeQuality(Object qualityValue) {
        if (qualityValue == null) {
            return "standard";
        }
        String quality = String.valueOf(qualityValue).toLowerCase().trim();
        return switch (quality) {
            case "high", "hd" -> "hd";
            case "low", "standard", "" -> "standard";
            default -> "standard"; // fallback для неизвестных значений
        };
    }

    private String ensurePath(String baseUrl, String defaultSuffix) {
        String normalizedBase = (baseUrl == null || baseUrl.isBlank())
            ? "https://api.openai.com/v1"
            : baseUrl;

        if (normalizedBase.endsWith(defaultSuffix)) {
            return normalizedBase;
        }
        if (normalizedBase.endsWith("/")) {
            return normalizedBase + defaultSuffix.substring(1);
        }
        return normalizedBase + defaultSuffix;
    }
}

