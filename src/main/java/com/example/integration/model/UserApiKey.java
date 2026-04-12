package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель для хранения пользовательских API ключей нейросетей
 * Ключи хранятся в связке: пользователь - сервис - нейросеть
 * Доступно только для платного тарифа
 */
@Entity
@Table(name = "user_api_keys", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"user_account_id", "client_application_id", "neural_network_id"}
       ))
@Data
@NoArgsConstructor
public class UserApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_application_id", nullable = false)
    private ClientApplication clientApplication;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neural_network_id", nullable = false)
    private NeuralNetwork neuralNetwork;
    
    @Column(name = "api_key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String apiKeyEncrypted; // Зашифрованный API ключ
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public UserApiKey(UserAccount userAccount, ClientApplication clientApplication, 
                     NeuralNetwork neuralNetwork, String apiKeyEncrypted) {
        this.userAccount = userAccount;
        this.clientApplication = clientApplication;
        this.neuralNetwork = neuralNetwork;
        this.apiKeyEncrypted = apiKeyEncrypted;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

