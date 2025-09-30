package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Клиент для GigaChat (Сбер)
 * API: https://developers.sber.ru/docs/ru/gigachat/api/overview
 */
@Component
public class GigaChatClient extends BaseNeuralClient {
    
    public GigaChatClient(RestTemplate restTemplate, ObjectMapper objectMapper, com.example.integration.security.EncryptionService encryptionService) {
        super(restTemplate, objectMapper, encryptionService);
    }
    
    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        // Применяем маппинг запроса
        Map<String, Object> requestBody = applyRequestMapping(payload, network.getRequestMapping());
        
        // Добавляем модель, если не указана
        if (!requestBody.containsKey("model")) {
            requestBody.put("model", network.getModelName() != null ? network.getModelName() : "GigaChat");
        }
        
        // Подготавливаем заголовки (GigaChat использует access token)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            // ✅ Расшифровываем ключ для GigaChat API
            String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
            headers.set("Authorization", "Bearer " + decryptedKey);
        }
        
        // Создаем запрос
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Отправляем запрос
        String url = network.getApiUrl();
        if (!url.contains("/chat/completions")) {
            url = url + "/v1/chat/completions";
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

