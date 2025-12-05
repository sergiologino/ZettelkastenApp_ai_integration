package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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
        // Применяем маппинг запроса
        Map<String, Object> requestBody = applyRequestMapping(payload, network.getRequestMapping());

        // Удаляем служебные поля, которые не понимает OpenAI
        requestBody.remove("mode");
        Object settingsRaw = requestBody.remove("settings");
        if (settingsRaw instanceof Map<?, ?> settingsMap) {
            Object temperature = settingsMap.get("temperature");
            if (temperature instanceof Number && !requestBody.containsKey("temperature")) {
                requestBody.put("temperature", ((Number) temperature).doubleValue());
            }
            Object maxTokens = settingsMap.get("maxTokens");
            if (maxTokens instanceof Number && !requestBody.containsKey("max_tokens")) {
                requestBody.put("max_tokens", ((Number) maxTokens).intValue());
            }
        }
        
        // Добавляем модель, если не указана
        if (!requestBody.containsKey("model")) {
            requestBody.put("model", network.getModelName() != null ? network.getModelName() : "gpt-4");
        }
        
        // Подготавливаем заголовки
        HttpHeaders headers = prepareHeaders(network);
        
        // Создаем запрос
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Отправляем запрос
        String url = network.getApiUrl();
        if (!url.endsWith("/chat/completions")) {
            url = url + "/chat/completions";
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

