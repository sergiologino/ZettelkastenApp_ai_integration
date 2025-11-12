package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Клиент для Whisper (транскрибация аудио)
 * API: https://platform.openai.com/docs/api-reference/audio
 */
@Component
public class WhisperClient extends BaseNeuralClient {
    
    public WhisperClient(RestTemplate restTemplate, ObjectMapper objectMapper, com.example.integration.security.EncryptionService encryptionService) {
        super(restTemplate, objectMapper, encryptionService);
    }
    
    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        // Извлекаем аудио-данные из payload
        String audioBase64 = (String) payload.get("audio");
        if (audioBase64 == null || audioBase64.isEmpty()) {
            throw new IllegalArgumentException("Audio data is required for transcription");
        }
        
        // Декодируем base64
        byte[] audioBytes = Base64.getDecoder().decode(audioBase64);
        
        // Подготавливаем multipart/form-data запрос
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            // ✅ Расшифровываем ключ для Whisper API
            String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
            headers.set("Authorization", "Bearer " + decryptedKey);
        }
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.mp3";
            }
        });
        body.add("model", network.getModelName() != null ? network.getModelName() : "whisper-1");
        
        // Добавляем опциональные параметры
        if (payload.containsKey("language")) {
            body.add("language", payload.get("language"));
        }
        if (payload.containsKey("prompt")) {
            body.add("prompt", payload.get("prompt"));
        }
        
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        
        // Отправляем запрос
        String url = network.getApiUrl();
        
        // ✅ ИСПРАВЛЕНО: Проверяем, содержит ли URL уже полный путь
        if (url.contains("/audio/transcriptions")) {
            // URL уже содержит полный путь, используем как есть
            System.out.println("🔍 [WhisperClient] URL уже содержит /audio/transcriptions: " + url);
        } else if (url.endsWith("/v1") || url.endsWith("/v1/")) {
            // URL заканчивается на /v1, добавляем только /audio/transcriptions
            url = url.replaceAll("/+$", "") + "/audio/transcriptions";
            System.out.println("✅ [WhisperClient] URL после добавления пути: " + url);
        } else {
            // URL не содержит /v1, добавляем полный путь
            url = url.replaceAll("/+$", "") + "/v1/audio/transcriptions";
            System.out.println("✅ [WhisperClient] URL после добавления /v1/audio/transcriptions: " + url);
        }
        
        System.out.println("🎤 [WhisperClient] Отправляем запрос к Whisper API: " + url);
        System.out.println("🎤 [WhisperClient] Model: " + (network.getModelName() != null ? network.getModelName() : "whisper-1"));
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            Map.class
        );
        
        System.out.println("✅ [WhisperClient] Получен ответ от Whisper API, status: " + response.getStatusCode());
        
        // Применяем маппинг ответа
        Map<String, Object> responseBody = response.getBody();
        return applyResponseMapping(responseBody, network.getResponseMapping());
    }
    
    @Override
    protected Integer extractTokensUsed(Map<String, Object> response) {
        // Whisper обычно не возвращает информацию о токенах
        // Можем оценить примерно по длине текста
        if (response.containsKey("text")) {
            String text = (String) response.get("text");
            return text.length() / 4; // Примерная оценка
        }
        return 0;
    }
}

