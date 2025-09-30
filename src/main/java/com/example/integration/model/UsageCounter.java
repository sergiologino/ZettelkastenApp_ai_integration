package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_counters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"external_user_id", "neural_network_id", "period_start"})
})
@Data
@NoArgsConstructor
public class UsageCounter {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_user_id", nullable = false)
    private ExternalUser externalUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neural_network_id", nullable = false)
    private NeuralNetwork neuralNetwork;
    
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    
    @Column(name = "request_count", nullable = false)
    private Integer requestCount = 0;
    
    @Column(name = "token_count", nullable = false)
    private Integer tokenCount = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public void incrementRequests(int tokensUsed) {
        this.requestCount++;
        this.tokenCount += tokensUsed;
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

