package com.example.integration.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class NetworkDTO {
    private String id;
    private String name;
    private String displayName;
    private String provider;
    private String networkType;
    private String apiUrl;
    private String modelName;
    private Boolean isActive;
    private Boolean isFree;
    private Integer priority;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private Map<String, Object> requestMapping;
    private Map<String, Object> responseMapping;
    private String connectionInstruction;
    private java.math.BigDecimal costPerTokenRub;
    private java.math.BigDecimal wordsPerToken;
    private java.math.BigDecimal secondsPerToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

