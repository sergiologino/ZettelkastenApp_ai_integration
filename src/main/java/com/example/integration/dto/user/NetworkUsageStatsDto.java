package com.example.integration.dto.user;

import lombok.Data;
import java.util.UUID;

/**
 * DTO для статистики использования нейросети клиентом
 */
@Data
public class NetworkUsageStatsDto {
    private UUID networkId;
    private String networkName;
    private String networkDisplayName;
    private String provider;
    private String networkType;
    
    // Статистика использования
    private Long totalRequests; // Всего запросов
    private Long successfulRequests; // Успешных запросов
    private Long failedRequests; // Неудачных запросов
    private Long totalTokensUsed; // Всего использовано токенов
    private Long availableTokens; // Доступно токенов (если есть лимит)
}

