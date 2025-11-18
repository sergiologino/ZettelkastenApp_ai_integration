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
    private LocalDateTime nextPaymentDate; // Плановая дата следующей оплаты
    
    public static SubscriptionInfoDto fromEntity(Subscription subscription) {
        SubscriptionInfoDto dto = new SubscriptionInfoDto();
        dto.setId(subscription.getId());
        dto.setPlan(SubscriptionPlanDto.fromEntity(subscription.getSubscriptionPlan()));
        dto.setStatus(subscription.getStatus().name());
        dto.setStartedAt(subscription.getStartedAt());
        dto.setExpiresAt(subscription.getExpiresAt());
        
        // Рассчитываем плановую дату следующей оплаты
        if (subscription.isActive() && subscription.getAutoRenew() && subscription.getSubscriptionPlan().getDurationDays() > 0) {
            // Если подписка активна и включено автопродление, следующая оплата = дата окончания
            dto.setNextPaymentDate(subscription.getExpiresAt());
        } else if (subscription.isActive() && subscription.getExpiresAt() != null) {
            // Если подписка активна, но автопродление выключено, следующая оплата = дата окончания
            dto.setNextPaymentDate(subscription.getExpiresAt());
        } else {
            dto.setNextPaymentDate(null);
        }
        
        return dto;
    }
}

