package com.example.integration.dto.user;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserApiKeyDto {
    private UUID id;
    private UUID clientApplicationId;
    private String clientApplicationName;
    private UUID neuralNetworkId;
    private String neuralNetworkName;
    private String neuralNetworkDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasKey; // true если ключ установлен (без показа самого ключа)
}

