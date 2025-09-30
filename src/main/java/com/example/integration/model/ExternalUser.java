package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"client_app_id", "external_user_id"})
})
@Data
@NoArgsConstructor
public class ExternalUser {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_app_id", nullable = false)
    private ClientApplication clientApp;
    
    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;
    
    @Column(name = "user_type", nullable = false, length = 50)
    private String userType = "free_user"; // new_user, free_user, paid_user
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

