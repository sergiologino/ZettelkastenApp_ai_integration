package com.example.integration.dto.subscription;

import com.example.integration.model.Subscription;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SubscriptionInfoDto {
    private UUID id;
    private SubscriptionPlanDto plan;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    
    public static SubscriptionInfoDto fromEntity(Subscription subscription) {
        SubscriptionInfoDto dto = new SubscriptionInfoDto();
        dto.setId(subscription.getId());
        dto.setPlan(SubscriptionPlanDto.fromEntity(subscription.getSubscriptionPlan()));
        dto.setStatus(subscription.getStatus().name());
        dto.setStartedAt(subscription.getStartedAt());
        dto.setExpiresAt(subscription.getExpiresAt());
        return dto;
    }
}

