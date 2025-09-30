package com.example.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientAppCreateRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
}

