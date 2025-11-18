package com.example.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO для группировки доступов по пользователям
 * Структура: Пользователь → Сервисы → Нейросети
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessGroupDto {
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private List<ClientServiceDto> services; // Сервисы пользователя
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public static class ClientServiceDto {
    private UUID clientId;
    private String clientName;
    private String clientDescription;
    private Boolean isAdminService; // true если это админский сервис (не привязан к пользователю)
    private List<NetworkAccessInfoDto> networks; // Нейросети в этом сервисе
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public static class NetworkAccessInfoDto {
    private UUID accessId;
    private UUID networkId;
    private String networkDisplayName;
    private String networkProvider;
    private String networkType;
    private Integer dailyRequestLimit;
    private Integer monthlyRequestLimit;
    private Integer priority;
}

