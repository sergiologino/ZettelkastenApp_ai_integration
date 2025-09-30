package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "network_limits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"neural_network_id", "user_type", "limit_period"})
})
@Data
@NoArgsConstructor
public class NetworkLimit {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neural_network_id", nullable = false)
    private NeuralNetwork neuralNetwork;
    
    @Column(name = "user_type", nullable = false, length = 50)
    private String userType; // new_user, free_user, paid_user
    
    @Column(name = "limit_period", nullable = false, length = 50)
    private String limitPeriod; // daily, monthly, yearly
    
    @Column(name = "request_limit")
    private Integer requestLimit; // NULL = unlimited
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

