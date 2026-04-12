package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Клиент для Yandex GPT
 * API: https://cloud.yandex.ru/docs/yandexgpt/
 */
@Component
public class YandexGptClient extends BaseNeuralClient {
    
    public YandexGptClient(RestTemplate restTemplate, ObjectMapper objectMapper, com.example.integration.security.EncryptionService encryptionService) {
        super(restTemplate, objectMapper, encryptionService);
    }
    
    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        // Применяем маппинг запроса
        Map<String, Object> requestBody = applyRequestMapping(payload, network.getRequestMapping());

        if ("speech_synthesis".equalsIgnoreCase(network.getNetworkType())) {
            return sendSpeechKitTtsRequest(network, requestBody);
        }

        // Добавляем modelUri, если не указан
        if (!requestBody.containsKey("modelUri")) {
            String modelUri = network.getModelName() != null 
                ? network.getModelName() 
                : "gpt://b1g6b7r9qqmq5g9b7q3r/yandexgpt-lite/latest";
            requestBody.put("modelUri", modelUri);
        }
        
        // Подготавливаем заголовки (Yandex использует API-ключ или IAM-токен)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            // ✅ Расшифровываем ключ для Yandex API
            String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
            headers.set("Authorization", "Api-Key " + decryptedKey);
        }
        
        // Создаем запрос
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Отправляем запрос
        String url = network.getApiUrl();
        if (!url.contains("/completion")) {
            url = url + "/foundationModels/v1/completion";
        }
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            Map.class
        );
        
        // Применяем маппинг ответа
        Map<String, Object> responseBody = response.getBody();
        return applyResponseMapping(responseBody, network.getResponseMapping());
    }

    /**
     * Yandex Cloud SpeechKit: синтез речи (REST API v1, {@code tts:synthesize}).
     * Тот же API-ключ Yandex Cloud, что и для Yandex GPT — заголовок {@code Authorization: Api-Key}.
     *
     * @see <a href="https://yandex.cloud/ru/docs/speechkit/tts/request">Документация SpeechKit TTS</a>
     */
    private Map<String, Object> sendSpeechKitTtsRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        String text = extractTtsText(payload);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Payload must include non-empty \"text\" or \"input\" for Yandex SpeechKit synthesis.");
        }

        String lang = payload.containsKey("lang") ? String.valueOf(payload.get("lang")) : "ru-RU";
        String voice = payload.containsKey("voice") ? String.valueOf(payload.get("voice")).trim() : "alena";
        String format = payload.containsKey("format") ? String.valueOf(payload.get("format")) : "oggopus";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("text", text);
        form.add("lang", lang);
        form.add("voice", voice);
        form.add("format", format);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
            headers.set("Authorization", "Api-Key " + decryptedKey);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        String url = resolveYandexTtsUrl(network.getApiUrl());

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
        result.put("format", format);
        result.put("lang", lang);
        result.put("voice", voice);
        result.put("provider", "yandex_speechkit");
        return applyResponseMapping(result, network.getResponseMapping());
    }

    private static String resolveYandexTtsUrl(String apiUrl) {
        if (apiUrl != null && !apiUrl.isBlank() && apiUrl.contains("tts:synthesize")) {
            return apiUrl.trim();
        }
        return "https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize";
    }

    private static String extractTtsText(Map<String, Object> payload) {
        if (payload.containsKey("text")) {
            Object v = payload.get("text");
            return v != null ? String.valueOf(v) : null;
        }
        if (payload.containsKey("input")) {
            Object v = payload.get("input");
            return v != null ? String.valueOf(v) : null;
        }
        return null;
    }
}

