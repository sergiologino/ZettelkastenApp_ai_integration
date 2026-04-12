package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель истории платежей
 */
@Entity
@Table(name = "payment_history")
@Data
@NoArgsConstructor
public class PaymentHistory {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 10)
    private String currency = "RUB";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "transaction_id", nullable = false, unique = true, length = 255)
    private String transactionId;
    
    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;
    
    @Column(name = "provider_name", length = 100)
    private String providerName = "YooKassa";
    
    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "webhook_data", columnDefinition = "TEXT")
    private String webhookData;
    
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    
    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;
    
    public PaymentHistory(UserAccount userAccount, SubscriptionPlan subscriptionPlan, 
                         BigDecimal amount, String currency, PaymentMethod paymentMethod) {
        this.userAccount = userAccount;
        this.subscriptionPlan = subscriptionPlan;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.transactionId = UUID.randomUUID().toString();
    }
    
    public void markCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }
    
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }
    
    public enum PaymentMethod {
        CARD,
        MOBILE_PAYMENT,
        BANK_TRANSFER,
        E_WALLET
    }
    
    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED
    }
}

