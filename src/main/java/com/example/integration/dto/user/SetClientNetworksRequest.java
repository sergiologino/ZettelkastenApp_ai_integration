package com.example.integration.dto.user;

import lombok.Data;
import java.util.List;
import java.util.UUID;

/**
 * DTO для установки списка нейросетей для клиента
 */
@Data
public class SetClientNetworksRequest {
    private List<UUID> networkIds; // Для обратной совместимости
    private List<NetworkWithPriority> networks; // Новый формат с приоритетами
}

