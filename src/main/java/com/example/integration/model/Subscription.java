package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель подписки пользователя
 */
@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
public class Subscription {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public Subscription(UserAccount userAccount, SubscriptionPlan subscriptionPlan) {
        this.userAccount = userAccount;
        this.subscriptionPlan = subscriptionPlan;
        this.startedAt = LocalDateTime.now();
        
        // Устанавливаем дату окончания для платных планов
        if (!subscriptionPlan.isFree() && subscriptionPlan.getDurationDays() > 0) {
            this.expiresAt = this.startedAt.plusDays(subscriptionPlan.getDurationDays());
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        if (status != SubscriptionStatus.ACTIVE) {
            return false;
        }
        if (expiresAt == null) {
            return true; // Бессрочная подписка
        }
        return LocalDateTime.now().isBefore(expiresAt);
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
        this.autoRenew = false;
    }
    
    public void renew() {
        if (subscriptionPlan.getDurationDays() > 0) {
            LocalDateTime newExpiryDate = (expiresAt != null && expiresAt.isAfter(LocalDateTime.now())) 
                ? expiresAt.plusDays(subscriptionPlan.getDurationDays())
                : LocalDateTime.now().plusDays(subscriptionPlan.getDurationDays());
            
            this.expiresAt = newExpiryDate;
            this.status = SubscriptionStatus.ACTIVE;
        }
    }
    
    public enum SubscriptionStatus {
        ACTIVE,
        EXPIRED,
        CANCELLED,
        SUSPENDED
    }
}

