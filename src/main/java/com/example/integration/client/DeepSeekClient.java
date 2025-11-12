package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Клиент для DeepSeek AI
 * API: https://platform.deepseek.com/api-docs/
 * 
 * Поддерживаемые модели:
 * - deepseek-chat: Универсальная модель для диалогов (~$0.0014/1M input tokens)
 * - deepseek-coder: Специализированная модель для кода (~$0.0014/1M input tokens)
 * - deepseek-v3: Последняя флагманская модель (MoE, 671B параметров, декабрь 2024)
 * 
 * Особенности:
 * - Очень низкая стоимость (~$0.0014 за 1M входных токенов)
 * - OpenAI-совместимый API
 * - Высокое качество для китайского и английского языков
 * - DeepSeek-V3 использует MoE архитектуру (Mixture of Experts)
 */
@Component
public class DeepSeekClient extends BaseNeuralClient {
    
    public DeepSeekClient(RestTemplate restTemplate, ObjectMapper objectMapper, com.example.integration.security.EncryptionService encryptionService) {
        super(restTemplate, objectMapper, encryptionService);
    }
    
    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        System.out.println("🔵 [DeepSeekClient] Отправляем запрос к DeepSeek API");
        System.out.println("🔵 [DeepSeekClient] Model: " + network.getModelName());
        
        // Применяем маппинг запроса
        Map<String, Object> requestBody = applyRequestMapping(payload, network.getRequestMapping());
        
        // Добавляем модель, если не указана
        if (!requestBody.containsKey("model")) {
            requestBody.put("model", network.getModelName() != null ? network.getModelName() : "deepseek-chat");
        }
        
        // Проверяем наличие API ключа
        System.out.println("🔑 [DeepSeekClient] Проверяем API ключ для DeepSeek:");
        System.out.println("🔑 [DeepSeekClient]   - Network ID: " + network.getId());
        System.out.println("🔑 [DeepSeekClient]   - API key encrypted присутствует: " + (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()));
        
        // Подготавливаем заголовки (DeepSeek использует "Authorization: Bearer <api-key>")
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            try {
                String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
                headers.set("Authorization", "Bearer " + decryptedKey);
                System.out.println("✅ [DeepSeekClient] Authorization header установлен");
            } catch (Exception e) {
                System.err.println("❌ [DeepSeekClient] Ошибка расшифровки API ключа: " + e.getMessage());
                throw new RuntimeException("Ошибка расшифровки API ключа для DeepSeek: " + e.getMessage(), e);
            }
        } else {
            System.err.println("❌ [DeepSeekClient] API ключ для DeepSeek отсутствует!");
            throw new RuntimeException("API ключ для DeepSeek отсутствует. Зарегистрируйтесь на https://platform.deepseek.com/ и добавьте API ключ в настройках нейросети.");
        }
        
        // Создаем запрос
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Формируем URL (DeepSeek использует OpenAI-совместимый endpoint)
        String url = network.getApiUrl();
        if (!url.contains("/chat/completions")) {
            if (url.endsWith("/v1") || url.endsWith("/v1/")) {
                url = url.replaceAll("/+$", "") + "/chat/completions";
            } else {
                url = url.replaceAll("/+$", "") + "/v1/chat/completions";
            }
        }
        
        System.out.println("🔵 [DeepSeekClient] URL: " + url);
        System.out.println("🔵 [DeepSeekClient] Request body keys: " + requestBody.keySet());
        
        // Отправляем запрос
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            Map.class
        );
        
        System.out.println("✅ [DeepSeekClient] Получен ответ от DeepSeek API, status: " + response.getStatusCode());
        
        // Применяем маппинг ответа
        Map<String, Object> responseBody = response.getBody();
        
        // Логируем структуру ответа для отладки
        if (responseBody != null) {
            System.out.println("🔵 [DeepSeekClient] Response keys: " + responseBody.keySet());
            if (responseBody.containsKey("choices")) {
                System.out.println("🔵 [DeepSeekClient] Choices count: " + ((java.util.List<?>) responseBody.get("choices")).size());
            }
        }
        
        return applyResponseMapping(responseBody, network.getResponseMapping());
    }
    
    @Override
    protected HttpHeaders prepareHeaders(NeuralNetwork network) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // DeepSeek использует Bearer token аутентификацию (как OpenAI)
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            try {
                String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
                headers.set("Authorization", "Bearer " + decryptedKey);
            } catch (Exception e) {
                System.err.println("❌ [DeepSeekClient] Ошибка расшифровки API ключа в prepareHeaders: " + e.getMessage());
                throw new RuntimeException("Ошибка расшифровки API ключа для DeepSeek", e);
            }
        }
        
        return headers;
    }
}

