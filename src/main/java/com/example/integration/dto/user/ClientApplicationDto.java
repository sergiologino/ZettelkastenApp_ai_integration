package com.example.integration.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ClientApplicationDto {
    private UUID id;
    private String name;
    private String description;
    private String apiKey;
    private Boolean isActive;
    private Boolean deleted;
    private List<UUID> networkIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<UUID> getNetworkIds() { return networkIds; }
    public void setNetworkIds(List<UUID> networkIds) { this.networkIds = networkIds; }
}


