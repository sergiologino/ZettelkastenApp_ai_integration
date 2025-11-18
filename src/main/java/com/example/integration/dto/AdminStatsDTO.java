package com.example.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Long totalTokensUsed;
    private BigDecimal totalCostRub; // Общая стоимость всех запросов в рублях
    private Map<String, Long> requestsByNetwork;
    private Map<String, Long> requestsByClient;
    private Map<String, Long> tokensByNetwork; // Токены по нейросетям
    private Map<String, BigDecimal> costByNetwork; // Стоимость по нейросетям
    private Map<String, Long> tokensByClient; // Токены по клиентам
    private Map<String, BigDecimal> costByClient; // Стоимость по клиентам
    private List<NetworkStatsDetailDto> networkDetails; // Детальная статистика по нейросетям
    private List<ClientStatsDetailDto> clientDetails; // Детальная статистика по клиентам
}

