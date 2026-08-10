package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        if ("speech_synthesis".equalsIgnoreCase(network.getNetworkType())) {
            return sendSpeechSynthesisRequest(network, mappedPayload);
        }

        if ("image_generation".equalsIgnoreCase(network.getNetworkType())) {
            return sendImageGenerationRequest(network, mappedPayload, settingsRaw);
        }

        if ("image_edit".equalsIgnoreCase(network.getNetworkType())) {
            return sendImageEditRequest(network, mappedPayload, settingsRaw);
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

    /**
     * OpenAI Text-to-Speech: {@code POST /v1/audio/speech}
     * <a href="https://platform.openai.com/docs/api-reference/audio/createSpeech">API reference</a>
     */
    private Map<String, Object> sendSpeechSynthesisRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        String input = extractSpeechInput(payload);
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Payload must include non-empty \"input\" or \"text\" for OpenAI speech synthesis.");
        }

        String voice = payload.containsKey("voice") ? String.valueOf(payload.get("voice")).trim() : "alloy";
        String model = resolveModel(payload, network, "tts-1");
        String responseFormat = payload.containsKey("response_format")
            ? String.valueOf(payload.get("response_format"))
            : "mp3";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", input);
        body.put("voice", voice);
        body.put("response_format", responseFormat);

        HttpHeaders headers = prepareHeaders(network);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = ensurePath(network.getApiUrl(), "/audio/speech");
        Objects.requireNonNull(url, "Resolved OpenAI TTS endpoint is null");

        ResponseEntity<byte[]> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            byte[].class
        );

        byte[] audio = response.getBody();
        String audioBase64 = audio != null && audio.length > 0
            ? Base64.getEncoder().encodeToString(audio)
            : "";

        Map<String, Object> result = new HashMap<>();
        result.put("audioBase64", audioBase64);
        result.put("format", responseFormat);
        result.put("voice", voice);
        result.put("model", model);
        result.put("provider", "openai");
        return applyResponseMapping(result, network.getResponseMapping());
    }

    private static String extractSpeechInput(Map<String, Object> payload) {
        if (payload.containsKey("input")) {
            Object v = payload.get("input");
            return v != null ? String.valueOf(v) : null;
        }
        if (payload.containsKey("text")) {
            Object v = payload.get("text");
            return v != null ? String.valueOf(v) : null;
        }
        return null;
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

    /**
     * OpenAI GPT Image edit endpoint. The source image is sent as a multipart file,
     * because /images/edits does not accept the JSON data-URL form used by chat vision.
     */
    private Map<String, Object> sendImageEditRequest(
        NeuralNetwork network,
        Map<String, Object> payload,
        Object settingsRaw
    ) throws Exception {
        String prompt = extractPrompt(payload);
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt is required for OpenAI image editing.");
        }
        String source = extractImageBase64(payload);
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Payload must include non-empty imageBase64 for OpenAI image editing.");
        }

        Map<String, Object> settings = extractSettings(settingsRaw);
        String model = resolveModel(payload, network, "gpt-image-1.5");
        String outputFormat = stringValue(payload.get("output_format"), stringValue(settings.get("outputFormat"), "jpeg"));
        String contentType = stringValue(payload.get("imageContentType"), "image/jpeg");
        byte[] imageBytes = decodeImageBase64(source);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", model);
        body.add("prompt", prompt);
        body.add("image", namedImageResource(imageBytes, contentType));
        body.add("input_fidelity", stringValue(payload.get("input_fidelity"), "high"));
        body.add("quality", normalizeGptImageQuality(payload.get("quality"), settings.get("quality")));
        body.add("output_format", outputFormat);
        String size = deriveImageEditSize(settings);
        if (size != null) {
            body.add("size", size);
        }

        HttpHeaders headers = prepareHeaders(network);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        String url = ensurePath(network.getApiUrl(), "/edits");
        Objects.requireNonNull(url, "Resolved OpenAI image edit endpoint is null");

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        Map<String, Object> normalized = normalizeImageEditResponse(response.getBody(), model, outputFormat);
        return applyResponseMapping(normalized, network.getResponseMapping());
    }

    private static String extractImageBase64(Map<String, Object> payload) {
        for (String key : List.of("imageBase64", "image", "sourceImageBase64")) {
            Object value = payload.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str.trim();
            }
        }
        return null;
    }

    private static byte[] decodeImageBase64(String value) {
        String normalized = value.trim();
        int comma = normalized.indexOf(',');
        if (normalized.startsWith("data:") && comma >= 0) {
            normalized = normalized.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("imageBase64 is not valid base64 data", ex);
        }
    }

    private static ByteArrayResource namedImageResource(byte[] bytes, String contentType) {
        String extension = contentType != null && contentType.toLowerCase(Locale.ROOT).contains("png") ? ".png" : ".jpg";
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "avatar-source" + extension;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> normalizeImageEditResponse(Map<String, Object> raw, String model, String outputFormat) {
        if (raw == null || !(raw.get("data") instanceof List<?> data) || data.isEmpty() || !(data.get(0) instanceof Map<?, ?> first)) {
            throw new IllegalStateException("OpenAI image edit returned no image data");
        }
        Object base64 = first.get("b64_json");
        if (!(base64 instanceof String value) || value.isBlank()) {
            throw new IllegalStateException("OpenAI image edit returned no base64 image");
        }
        String contentType = "image/" + ("jpg".equalsIgnoreCase(outputFormat) ? "jpeg" : outputFormat);
        Map<String, Object> image = new HashMap<>();
        image.put("base64", value);
        image.put("contentType", contentType);

        Map<String, Object> result = new HashMap<>();
        result.put("provider", "openai");
        result.put("model", model);
        result.put("imageBase64", value);
        result.put("imageContentType", contentType);
        result.put("data", List.of(image));
        result.put("output", List.of(image));
        if (raw.get("usage") != null) {
            result.put("usage", raw.get("usage"));
        }
        return result;
    }

    private static String stringValue(Object value, String fallback) {
        if (value instanceof String str && !str.isBlank()) {
            return str.trim();
        }
        return fallback;
    }

    private static String normalizeGptImageQuality(Object directValue, Object settingsValue) {
        String raw = stringValue(directValue, stringValue(settingsValue, "medium")).toLowerCase(Locale.ROOT);
        return switch (raw) {
            case "low", "medium", "high", "auto" -> raw;
            case "standard" -> "medium";
            case "hd" -> "high";
            default -> "medium";
        };
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
    private String deriveImageEditSize(Map<String, Object> settings) {
        Integer width = toInt(settings.get("width"));
        Integer height = toInt(settings.get("height"));
        if (width != null && height != null) {
            return normalizeEditSizeByAspect(width, height);
        }
        Object ratio = settings.get("aspectRatio");
        if (ratio instanceof String str && !str.isBlank()) {
            return switch (str) {
                case "16:9" -> "1536x1024";
                case "9:16", "3:4", "4:5" -> "1024x1536";
                default -> "1024x1024";
            };
        }
        return null;
    }

    private String normalizeEditSizeByAspect(int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        double aspect = (double) width / (double) height;
        if (Math.abs(aspect - 1.0) < 0.05) {
            return "1024x1024";
        }
        if (aspect > 1.0) {
            return "1536x1024";
        }
        return "1024x1536";
    }
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
            : ensureUrlHasScheme(baseUrl.trim());

        if (normalizedBase.endsWith(defaultSuffix)) {
            return normalizedBase;
        }
        if (normalizedBase.endsWith("/")) {
            return normalizedBase + defaultSuffix.substring(1);
        }
        return normalizedBase + defaultSuffix;
    }

    /**
     * В БД иногда сохраняют {@code api.openai.com/v1} без {@code https://} — тогда RestTemplate падает с «URI with undefined scheme».
     */
    private static String ensureUrlHasScheme(String url) {
        if (url.contains("://")) {
            return url;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("localhost") || lower.startsWith("127.0.0.1")) {
            return "http://" + url;
        }
        return "https://" + url;
    }
}
