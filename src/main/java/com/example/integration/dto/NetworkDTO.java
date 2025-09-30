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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

