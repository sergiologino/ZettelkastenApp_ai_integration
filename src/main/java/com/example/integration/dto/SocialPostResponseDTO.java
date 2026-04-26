package com.example.integration.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SocialPostResponseDTO {
    private String requestId;
    private String status;
    private String platform;
    private String providerPostId;
    private Map<String, Object> response;
    private String errorMessage;
    private Integer executionTimeMs;
}
