package com.example.integration.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class SaveUserApiKeyRequest {
    @NotNull
    private UUID clientId;
    
    @NotNull
    private UUID networkId;
    
    @NotBlank
    private String apiKey;
}

