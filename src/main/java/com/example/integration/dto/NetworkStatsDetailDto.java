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
public class NetworkStatsDetailDto {
    private UUID networkId;
    private String networkName;
    private String networkDisplayName;
    private String provider;
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Long totalTokensUsed;
    private BigDecimal totalCostRub; // Общая стоимость в рублях
    private BigDecimal costPerTokenRub; // Себестоимость токена
    private Map<String, Long> requestsByClient; // Запросы по клиентам
    private Map<String, Long> tokensByClient; // Токены по клиентам
    private Map<String, BigDecimal> costByClient; // Стоимость по клиентам
}

