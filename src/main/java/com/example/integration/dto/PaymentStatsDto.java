package com.example.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsDto {
    private UUID paymentId;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String planName;
    private String planDisplayName;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String transactionId;
}

