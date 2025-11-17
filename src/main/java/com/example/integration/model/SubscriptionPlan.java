package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель тарифного плана подписки
 */
@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
public class SubscriptionPlan {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name; // "FREE", "PAID"
    
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false, length = 10)
    private String currency = "RUB";
    
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays; // 0 - бессрочно, 30 - месяц
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isFree() {
        return price.compareTo(BigDecimal.ZERO) == 0;
    }
}

