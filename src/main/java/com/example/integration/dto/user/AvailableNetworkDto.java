package com.example.integration.dto.user;

import lombok.Data;
import java.util.UUID;

/**
 * DTO для доступных нейросетей для пользователей
 */
@Data
public class AvailableNetworkDto {
    private UUID id;
    private String code; // name из neural_networks
    private String label; // displayName из neural_networks
    private String provider;
    private String networkType;
    private String connectionInstruction; // Инструкция по подключению
}

