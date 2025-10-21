package com.example.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Long totalTokensUsed;
    private Map<String, Long> requestsByNetwork;
    private Map<String, Long> requestsByClient;
}

