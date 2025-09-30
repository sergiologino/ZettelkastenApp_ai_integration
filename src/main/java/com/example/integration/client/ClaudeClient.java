package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Клиент для Anthropic Claude
 * API: https://docs.anthropic.com/claude/reference
 */
@Component
public class ClaudeClient extends BaseNeuralClient {
    
    public ClaudeClient(RestTemplate restTemplate, ObjectMapper objectMapper, com.example.integration.security.EncryptionService encryptionService) {
        super(restTemplate, objectMapper, encryptionService);
    }
    
    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        // Применяем маппинг запроса
        Map<String, Object> requestBody = applyRequestMapping(payload, network.getRequestMapping());
        
        // Добавляем модель, если не указана
        if (!requestBody.containsKey("model")) {
            requestBody.put("model", network.getModelName() != null ? network.getModelName() : "claude-3-opus-20240229");
        }
        
        // Добавляем max_tokens, если не указан
        if (!requestBody.containsKey("max_tokens")) {
            requestBody.put("max_tokens", 4096);
        }
        
        // Подготавливаем заголовки (Claude использует x-api-key)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("anthropic-version", "2023-06-01");
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            // ✅ Расшифровываем ключ для Claude API
            String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
            headers.set("x-api-key", decryptedKey);
        }
        
        // Создаем запрос
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Отправляем запрос
        String url = network.getApiUrl();
        if (!url.endsWith("/messages")) {
            url = url + "/v1/messages";
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

