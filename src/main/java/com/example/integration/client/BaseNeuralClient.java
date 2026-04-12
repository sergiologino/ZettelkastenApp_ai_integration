package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.security.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Базовый класс для всех клиентов нейросетей
 */
public abstract class BaseNeuralClient {
    
    // ThreadLocal для передачи пользовательского API ключа
    private static final ThreadLocal<String> userApiKeyHolder = new ThreadLocal<>();
    
    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;
    protected final EncryptionService encryptionService;
    
    public BaseNeuralClient(
        RestTemplate restTemplate, 
        ObjectMapper objectMapper,
        EncryptionService encryptionService
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.encryptionService = encryptionService;
    }
    
    /**
     * Отправка запроса к нейросети
     */
    public abstract Map<String, Object> sendRequest(
        NeuralNetwork network, 
        Map<String, Object> payload
    ) throws Exception;
    
    /**
     * Установить пользовательский API ключ для текущего потока
     */
    public static void setUserApiKey(String userApiKey) {
        if (userApiKey != null && !userApiKey.isEmpty()) {
            userApiKeyHolder.set(userApiKey);
        } else {
            userApiKeyHolder.remove();
        }
    }
    
    /**
     * Очистить пользовательский API ключ для текущего потока
     */
    public static void clearUserApiKey() {
        userApiKeyHolder.remove();
    }
    
    /**
     * Подготовка заголовков запроса
     * Сначала проверяет пользовательский ключ из ThreadLocal, затем системный
     */
    protected HttpHeaders prepareHeaders(NeuralNetwork network) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Сначала используем пользовательский ключ из ThreadLocal, если он есть
        String userApiKey = userApiKeyHolder.get();
        if (userApiKey != null && !userApiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + userApiKey);
        } else if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
            // ✅ Расшифровываем системный API ключ перед использованием
            String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
            headers.set("Authorization", "Bearer " + decryptedKey);
        }
        
        return headers;
    }
    
    /**
     * Применение маппинга к запросу
     */
    protected Map<String, Object> applyRequestMapping(
        Map<String, Object> payload, 
        Map<String, Object> mapping
    ) {
        if (mapping == null || mapping.isEmpty()) {
            return payload;
        }
        
        // Простой маппинг (можно расширить для сложных случаев)
        return payload;
    }
    
    /**
     * Применение маппинга к ответу
     */
    protected Map<String, Object> applyResponseMapping(
        Map<String, Object> response, 
        Map<String, Object> mapping
    ) {
        if (mapping == null || mapping.isEmpty()) {
            return response;
        }
        
        // Простой маппинг (можно расширить для сложных случаев)
        return response;
    }
    
    /**
     * Извлечение количества использованных токенов из ответа
     */
    protected Integer extractTokensUsed(Map<String, Object> response) {
        // Попытка извлечь токены из стандартных полей
        if (response.containsKey("usage")) {
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            if (usage.containsKey("total_tokens")) {
                return ((Number) usage.get("total_tokens")).intValue();
            }
        }
        return 0;
    }
}

