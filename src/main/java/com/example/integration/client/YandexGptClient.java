package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
}

