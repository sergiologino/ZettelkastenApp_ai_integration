package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.security.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PollinationsClient extends BaseNeuralClient {

    private static final Logger log = LoggerFactory.getLogger(PollinationsClient.class);
    private static final String FALLBACK_CDN = "https://image.pollinations.ai/prompt/";

    public PollinationsClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        EncryptionService encryptionService
    ) {
        super(restTemplate, objectMapper, encryptionService);
    }

    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        log.info("🎨 [PollinationsClient] Обработка запроса для сети {}", network.getName());

        String prompt = extractPrompt(payload);
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Pollinations требует prompt для генерации изображения");
        }

        Map<String, Object> requestBody = buildRequestBody(network, payload, prompt);
        Integer width = getInteger(requestBody.get("width"));
        Integer height = getInteger(requestBody.get("height"));

        HttpHeaders headers = prepareHeaders(network);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        String endpoint = network.getApiUrl();
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Pollinations API URL не настроен");
        }

        HttpMethod method = HttpMethod.POST;

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                endpoint,
                Objects.requireNonNull(method),
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            log.debug("🎨 [PollinationsClient] API ответ: {}", body != null ? body.keySet() : "null");
            return normalizeResponse(prompt, requestBody, body, width, height);
        } catch (Exception apiError) {
            log.warn("⚠️ [PollinationsClient] Не удалось вызвать API, используем CDN: {}", apiError.getMessage());
            return buildFallbackResponse(prompt, requestBody, width, height);
        }
    }

    private Map<String, Object> buildRequestBody(NeuralNetwork network, Map<String, Object> payload, String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);

        Map<String, Object> settings = extractSettings(payload);
        Integer width = settings != null ? getInteger(settings.get("width")) : null;
        Integer height = settings != null ? getInteger(settings.get("height")) : null;
        if (width != null) {
            body.put("width", width);
        }
        if (height != null) {
            body.put("height", height);
        }
        if (settings != null) {
            String ratio = getString(settings.get("aspectRatio"));
            if (ratio != null) {
                body.put("ratio", ratio.toLowerCase(Locale.ENGLISH));
            }
            String quality = getString(settings.get("quality"));
            if (quality != null) {
                body.put("quality", quality);
            }
            String style = getString(settings.get("style"));
            if (style != null) {
                body.put("style", style);
            }
            Object seed = settings.get("seed");
            if (seed instanceof Number number) {
                body.put("seed", number.intValue());
            }
        }

        if (payload != null) {
            Object negativePrompt = payload.get("negative_prompt");
            if (negativePrompt instanceof String neg && !neg.isBlank()) {
                body.put("negative_prompt", neg);
            }
        }

        if (network.getModelName() != null && !network.getModelName().isBlank()) {
            body.putIfAbsent("model", network.getModelName());
        }

        body.putIfAbsent("nologo", true);
        return applyRequestMapping(body, network.getRequestMapping());
    }

    private Map<String, Object> normalizeResponse(
        String prompt,
        Map<String, Object> requestBody,
        Map<String, Object> rawResponse,
        Integer width,
        Integer height
    ) {
        List<String> assets = extractAssetUrls(rawResponse);
        if (assets.isEmpty()) {
            assets.add(buildCdnUrl(prompt, width, height));
        }

        Map<String, Object> normalized = new HashMap<>();
        normalized.put("provider", "pollinations");
        normalized.put("prompt", prompt);
        normalized.put("request", requestBody);
        normalized.put("rawResponse", rawResponse);
        normalized.put("assets", assets);
        normalized.put("output", assets.stream().map(url -> Map.of("url", url)).toList());
        normalized.put("status", rawResponse != null ? rawResponse.getOrDefault("status", "success") : "success");
        normalized.put("tokensUsed", 0);
        return normalized;
    }

    private Map<String, Object> buildFallbackResponse(
        String prompt,
        Map<String, Object> requestBody,
        Integer width,
        Integer height
    ) {
        String fallbackUrl = buildCdnUrl(prompt, width, height);
        Map<String, Object> raw = Map.of(
            "status", "fallback",
            "output", List.of(Map.of("url", fallbackUrl))
        );
        return normalizeResponse(prompt, requestBody, raw, width, height);
    }

    private List<String> extractAssetUrls(Map<String, Object> rawResponse) {
        List<String> assets = new ArrayList<>();
        if (rawResponse == null || rawResponse.isEmpty()) {
            return assets;
        }

        Object output = rawResponse.get("output");
        if (output instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof Map<?, ?> map) {
                    Object url = map.get("url");
                    if (url instanceof String str && !str.isBlank()) {
                        assets.add(str);
                    }
                }
            }
        }

        Object images = rawResponse.get("images");
        if (images instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof Map<?, ?> map) {
                    Object url = map.get("url");
                    if (url instanceof String str && !str.isBlank()) {
                        assets.add(str);
                    }
                } else if (item instanceof String str) {
                    assets.add(str);
                }
            }
        }

        Object directUrl = rawResponse.get("url");
        if (directUrl instanceof String str && !str.isBlank()) {
            assets.add(str);
        }

        return assets;
    }

    private String extractPrompt(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object prompt = payload.get("prompt");
        if (prompt instanceof String str && !str.isBlank()) {
            return str;
        }
        Object messages = payload.get("messages");
        if (messages instanceof List<?> list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Object entry = list.get(i);
                if (entry instanceof Map<?, ?> msg) {
                    Object role = msg.get("role");
                    if ("user".equals(role)) {
                        Object content = msg.get("content");
                        if (content instanceof String str && !str.isBlank()) {
                            return str;
                        }
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSettings(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object settings = payload.get("settings");
        if (settings instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private Integer getInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String getString(Object value) {
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }
        return null;
    }

    private String buildCdnUrl(String prompt, Integer width, Integer height) {
        String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder(FALLBACK_CDN).append(encodedPrompt);
        List<String> query = new ArrayList<>();
        if (width != null && width > 0) {
            query.add("width=" + width);
        }
        if (height != null && height > 0) {
            query.add("height=" + height);
        }
        if (!query.isEmpty()) {
            builder.append("?").append(String.join("&", query));
        }
        return builder.toString();
    }
}

