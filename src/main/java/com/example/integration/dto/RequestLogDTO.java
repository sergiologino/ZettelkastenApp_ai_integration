package com.example.integration.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RequestLogDTO {
    private String id;
    private String clientAppName;
    private String externalUserId;
    private String networkName;
    private String requestType;
    private Map<String, Object> requestPayload;
    private Map<String, Object> responsePayload;
    private String status;
    private String errorMessage;
    private Integer executionTimeMs;
    private Integer tokensUsed;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

