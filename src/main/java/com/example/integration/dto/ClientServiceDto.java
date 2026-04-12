package com.example.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientServiceDto {
    private UUID clientId;
    private String clientName;
    private String clientDescription;
    private Boolean isAdminService; // true если это админский сервис (не привязан к пользователю)
    private List<NetworkAccessInfoDto> networks; // Нейросети в этом сервисе
}

