package com.example.integration.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStatsDetailDto {
    private String provider;
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Long totalTokensUsed;
    private BigDecimal totalCostUsd;
    private BigDecimal totalCostRub;
}
