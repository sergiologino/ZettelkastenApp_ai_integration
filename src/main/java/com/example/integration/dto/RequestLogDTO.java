package com.example.integration.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RequestLogDTO {
    public UUID id;
    public String externalUserId;
    public UUID neuralNetworkId;
    public String neuralNetworkName;
    public UUID clientApplicationId;
    public String clientApplicationName;
    public String requestType;
    public String prompt;
    public String response;
    public boolean success;
    public String errorMessage;
    public Integer tokensUsed;
    public OffsetDateTime createdAt;

    public RequestLogDTO() {}

    public RequestLogDTO(
            UUID id,
            String externalUserId,
            UUID neuralNetworkId,
            String neuralNetworkName,
            UUID clientApplicationId,
            String clientApplicationName,
            String requestType,
            String prompt,
            String response,
            boolean success,
            String errorMessage,
            Integer tokensUsed,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.externalUserId = externalUserId;
        this.neuralNetworkId = neuralNetworkId;
        this.neuralNetworkName = neuralNetworkName;
        this.clientApplicationId = clientApplicationId;
        this.clientApplicationName = clientApplicationName;
        this.requestType = requestType;
        this.prompt = prompt;
        this.response = response;
        this.success = success;
        this.errorMessage = errorMessage;
        this.tokensUsed = tokensUsed;
        this.createdAt = createdAt;
    }
}
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

