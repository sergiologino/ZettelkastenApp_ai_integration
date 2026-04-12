package com.example.integration.dto.subscription;

import com.example.integration.model.SubscriptionPlan;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SubscriptionPlanDto {
    private UUID id;
    private String name;
    private String displayName;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer durationDays;
    private Boolean isActive;
    
    public static SubscriptionPlanDto fromEntity(SubscriptionPlan plan) {
        SubscriptionPlanDto dto = new SubscriptionPlanDto();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setDisplayName(plan.getDisplayName());
        dto.setDescription(plan.getDescription());
        dto.setPrice(plan.getPrice());
        dto.setCurrency(plan.getCurrency());
        dto.setDurationDays(plan.getDurationDays());
        dto.setIsActive(plan.getIsActive());
        return dto;
    }
}

