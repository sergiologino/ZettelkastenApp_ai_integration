package com.example.integration.dto;

/**
 * DTO для передачи информации о доступных нейросетях клиентам
 */
public class AvailableNetworkDTO {
    
    private String id;
    private String name;
    private String displayName;
    private String provider;
    private String networkType;
    private String modelName;
    private Boolean isFree;
    private Integer priority;
    private Integer remainingRequestsToday;
    private Integer remainingRequestsMonth;
    private Boolean hasLimits;
    
    // Конструкторы
    public AvailableNetworkDTO() {}
    
    public AvailableNetworkDTO(String id, String name, String displayName, String provider, String networkType) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.provider = provider;
        this.networkType = networkType;
    }
    
    // Геттеры и сеттеры
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getNetworkType() {
        return networkType;
    }
    
    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public Boolean getIsFree() {
        return isFree;
    }
    
    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Integer getRemainingRequestsToday() {
        return remainingRequestsToday;
    }
    
    public void setRemainingRequestsToday(Integer remainingRequestsToday) {
        this.remainingRequestsToday = remainingRequestsToday;
    }
    
    public Integer getRemainingRequestsMonth() {
        return remainingRequestsMonth;
    }
    
    public void setRemainingRequestsMonth(Integer remainingRequestsMonth) {
        this.remainingRequestsMonth = remainingRequestsMonth;
    }
    
    public Boolean getHasLimits() {
        return hasLimits;
    }
    
    public void setHasLimits(Boolean hasLimits) {
        this.hasLimits = hasLimits;
    }
}
