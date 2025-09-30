package com.example.integration.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AiResponseDTO {
    private String requestId; // ID запроса в логах
    private String status; // success, failed, rate_limited
    private String networkUsed; // Название использованной нейросети
    private Map<String, Object> response; // Ответ от нейросети
    private String errorMessage; // Сообщение об ошибке (если есть)
    private Integer executionTimeMs; // Время выполнения
    private Integer tokensUsed; // Использовано токенов
    private UsageLimitInfo usageLimitInfo; // Информация о лимитах
    
    @Data
    public static class UsageLimitInfo {
        private Integer used; // Использовано запросов
        private Integer limit; // Лимит (null = unlimited)
        private Integer remaining; // Осталось (null = unlimited)
        private String period; // daily, monthly, yearly
    }
}

