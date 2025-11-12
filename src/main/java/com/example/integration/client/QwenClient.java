package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Клиент для Qwen (Alibaba Cloud)
 * API: https://help.aliyun.com/zh/dashscope/developer-reference/api-details
 * 
 * Поддерживаемые модели:
 * - qwen-turbo: Быстрая и дешевая модель (~$0.002/1K tokens)
 * - qwen-plus: Сбалансированная модель (~$0.008/1K tokens)
 * - qwen-max: Самая мощная модель (~$0.02/1K tokens)
 * - qwen2.5-72b-instruct: Последняя версия с открытыми весами
 * 
 * Примечание: Qwen использует OpenAI-совместимый API
 */
@Component
public class QwenClient extends BaseNeuralClient {
    
    public QwenClient(RestTemplate restTemplate, ObjectMapper objectMapper, com.example.integration.security.EncryptionService encryptionService) {
        super(restTemplate, objectMapper, encryptionService);
    }
    
    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        System.out.println("🔵 [QwenClient] Отправляем запрос к Qwen API");
        System.out.println("🔵 [QwenClient] Model: " + network.getModelName());
        
        // Применяем маппинг запроса
        Map<String, Object> requestBody = applyRequestMapping(payload, network.getRequestMapping());
        
        // Добавляем модель, если не указана
        if (!requestBody.containsKey("model")) {
            requestBody.put("model", network.getModelName() != null ? network.getModelName() : "qwen-turbo");
        }
        
        // Проверяем наличие API ключа
        System.out.println("🔑 [QwenClient] Проверяем API ключ для Qwen:");
        System.out.println("🔑 [QwenClient]   - Network ID: " + network.getId());
        System.out.println("🔑 [QwenClient]   - API key encrypted присутствует: " + (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()));
        
        // Подготавливаем заголовки (Qwen использует "Authorization: Bearer <api-key>")
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            try {
                String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
                headers.set("Authorization", "Bearer " + decryptedKey);
                System.out.println("✅ [QwenClient] Authorization header установлен");
            } catch (Exception e) {
                System.err.println("❌ [QwenClient] Ошибка расшифровки API ключа: " + e.getMessage());
                throw new RuntimeException("Ошибка расшифровки API ключа для Qwen: " + e.getMessage(), e);
            }
        } else {
            System.err.println("❌ [QwenClient] API ключ для Qwen отсутствует!");
            throw new RuntimeException("API ключ для Qwen отсутствует. Добавьте API ключ DashScope (Alibaba Cloud) в настройках нейросети.");
        }
        
        // Создаем запрос
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Формируем URL (Qwen использует OpenAI-совместимый endpoint)
        String url = network.getApiUrl();
        if (!url.contains("/chat/completions")) {
            if (url.endsWith("/v1") || url.endsWith("/v1/")) {
                url = url.replaceAll("/+$", "") + "/chat/completions";
            } else {
                url = url.replaceAll("/+$", "") + "/v1/chat/completions";
            }
        }
        
        System.out.println("🔵 [QwenClient] URL: " + url);
        System.out.println("🔵 [QwenClient] Request body keys: " + requestBody.keySet());
        
        // Отправляем запрос
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            Map.class
        );
        
        System.out.println("✅ [QwenClient] Получен ответ от Qwen API, status: " + response.getStatusCode());
        
        // Применяем маппинг ответа
        Map<String, Object> responseBody = response.getBody();
        return applyResponseMapping(responseBody, network.getResponseMapping());
    }
    
    @Override
    protected HttpHeaders prepareHeaders(NeuralNetwork network) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Qwen использует Bearer token аутентификацию (как OpenAI)
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
            headers.set("Authorization", "Bearer " + decryptedKey);
        }
        
        return headers;
    }
}

