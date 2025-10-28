package com.example.integration.dto;

import java.util.UUID;

/**
 * DTO для запроса на предоставление или обновление доступа клиента к нейросети
 */
public class GrantAccessRequest {
    
    private UUID clientId;
    private UUID networkId;
    private Integer dailyRequestLimit;
    private Integer monthlyRequestLimit;

    // Конструкторы
    public GrantAccessRequest() {}

    public GrantAccessRequest(UUID clientId, UUID networkId, Integer dailyRequestLimit, Integer monthlyRequestLimit) {
        this.clientId = clientId;
        this.networkId = networkId;
        this.dailyRequestLimit = dailyRequestLimit;
        this.monthlyRequestLimit = monthlyRequestLimit;
    }

    // Геттеры и сеттеры
    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public Integer getDailyRequestLimit() {
        return dailyRequestLimit;
    }

    public void setDailyRequestLimit(Integer dailyRequestLimit) {
        this.dailyRequestLimit = dailyRequestLimit;
    }

    public Integer getMonthlyRequestLimit() {
        return monthlyRequestLimit;
    }

    public void setMonthlyRequestLimit(Integer monthlyRequestLimit) {
        this.monthlyRequestLimit = monthlyRequestLimit;
    }

    // Вспомогательные методы
    public boolean hasDailyLimit() {
        return dailyRequestLimit != null && dailyRequestLimit > 0;
    }

    public boolean hasMonthlyLimit() {
        return monthlyRequestLimit != null && monthlyRequestLimit > 0;
    }

    public boolean isUnlimited() {
        return !hasDailyLimit() && !hasMonthlyLimit();
    }

    @Override
    public String toString() {
        return "GrantAccessRequest{" +
                "clientId=" + clientId +
                ", networkId=" + networkId +
                ", dailyRequestLimit=" + dailyRequestLimit +
                ", monthlyRequestLimit=" + monthlyRequestLimit +
                '}';
    }
}
