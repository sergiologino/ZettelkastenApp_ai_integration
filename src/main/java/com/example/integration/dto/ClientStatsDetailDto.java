package com.example.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientStatsDetailDto {
    private UUID clientId;
    private String clientName;
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Long totalTokensUsed;
    private BigDecimal totalCostRub; // Общая стоимость в рублях
    private Map<String, Long> requestsByNetwork; // Запросы по нейросетям
    private Map<String, Long> tokensByNetwork; // Токены по нейросетям
    private Map<String, BigDecimal> costByNetwork; // Стоимость по нейросетям
}

