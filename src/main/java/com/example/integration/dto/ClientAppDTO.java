package com.example.integration.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClientAppDTO {
    private String id;
    private String name;
    private String description;
    private String apiKey; // Показываем только при создании
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

