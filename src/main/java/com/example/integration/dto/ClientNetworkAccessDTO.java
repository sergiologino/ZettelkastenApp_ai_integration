package com.example.integration.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO для передачи информации о доступе клиента к нейросети
 */
public class ClientNetworkAccessDTO {
    
    private UUID id;
    private UUID clientId;
    private String clientName;
    private UUID networkId;
    private String networkDisplayName;
    private String networkProvider;
    private String networkType;
    private Integer dailyRequestLimit;
    private Integer monthlyRequestLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Конструкторы
    public ClientNetworkAccessDTO() {}

    public ClientNetworkAccessDTO(UUID id, UUID clientId, String clientName, UUID networkId, 
                                 String networkDisplayName, String networkProvider, String networkType,
                                 Integer dailyRequestLimit, Integer monthlyRequestLimit,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.networkId = networkId;
        this.networkDisplayName = networkDisplayName;
        this.networkProvider = networkProvider;
        this.networkType = networkType;
        this.dailyRequestLimit = dailyRequestLimit;
        this.monthlyRequestLimit = monthlyRequestLimit;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Геттеры и сеттеры
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public String getNetworkDisplayName() {
        return networkDisplayName;
    }

    public void setNetworkDisplayName(String networkDisplayName) {
        this.networkDisplayName = networkDisplayName;
    }

    public String getNetworkProvider() {
        return networkProvider;
    }

    public void setNetworkProvider(String networkProvider) {
        this.networkProvider = networkProvider;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getLimitsDescription() {
        if (isUnlimited()) {
            return "Неограниченно";
        }
        
        StringBuilder sb = new StringBuilder();
        if (hasDailyLimit()) {
            sb.append(dailyRequestLimit).append(" дневных");
        }
        if (hasDailyLimit() && hasMonthlyLimit()) {
            sb.append(", ");
        }
        if (hasMonthlyLimit()) {
            sb.append(monthlyRequestLimit).append(" месячных");
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ClientNetworkAccessDTO{" +
                "id=" + id +
                ", clientName='" + clientName + '\'' +
                ", networkDisplayName='" + networkDisplayName + '\'' +
                ", dailyRequestLimit=" + dailyRequestLimit +
                ", monthlyRequestLimit=" + monthlyRequestLimit +
                '}';
    }
}
