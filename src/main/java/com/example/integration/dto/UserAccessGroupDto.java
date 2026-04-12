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

