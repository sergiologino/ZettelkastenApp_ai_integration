package com.example.integration.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель для связи клиентских приложений с нейросетями
 * Определяет, какие нейросети доступны конкретному клиенту и с какими лимитами
 */
@Entity
@Table(name = "client_network_access")
public class ClientNetworkAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_application_id", nullable = false)
    private ClientApplication clientApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neural_network_id", nullable = false)
    private NeuralNetwork neuralNetwork;

    @Column(name = "daily_request_limit")
    private Integer dailyRequestLimit;

    @Column(name = "monthly_request_limit")
    private Integer monthlyRequestLimit;
    
    @Column(name = "free_request_limit")
    private Integer freeRequestLimit; // Лимит бесплатных запросов для бесплатного плана

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Конструкторы
    public ClientNetworkAccess() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ClientNetworkAccess(ClientApplication clientApplication, NeuralNetwork neuralNetwork) {
        this();
        this.clientApplication = clientApplication;
        this.neuralNetwork = neuralNetwork;
    }

    public ClientNetworkAccess(ClientApplication clientApplication, NeuralNetwork neuralNetwork, 
                             Integer dailyRequestLimit, Integer monthlyRequestLimit) {
        this(clientApplication, neuralNetwork);
        this.dailyRequestLimit = dailyRequestLimit;
        this.monthlyRequestLimit = monthlyRequestLimit;
    }

    // Геттеры и сеттеры
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ClientApplication getClientApplication() {
        return clientApplication;
    }

    public void setClientApplication(ClientApplication clientApplication) {
        this.clientApplication = clientApplication;
    }

    public NeuralNetwork getNeuralNetwork() {
        return neuralNetwork;
    }

    public void setNeuralNetwork(NeuralNetwork neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }

    public Integer getDailyRequestLimit() {
        return dailyRequestLimit;
    }

    public void setDailyRequestLimit(Integer dailyRequestLimit) {
        this.dailyRequestLimit = dailyRequestLimit;
    }

    public Integer getMonthlyRequestLimit() {
        return monthlyRequestLimit;
    }

    public void setMonthlyRequestLimit(Integer monthlyRequestLimit) {
        this.monthlyRequestLimit = monthlyRequestLimit;
    }
    
    public Integer getFreeRequestLimit() {
        return freeRequestLimit;
    }
    
    public void setFreeRequestLimit(Integer freeRequestLimit) {
        this.freeRequestLimit = freeRequestLimit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Вспомогательные методы
    public boolean hasDailyLimit() {
        return dailyRequestLimit != null && dailyRequestLimit > 0;
    }

    public boolean hasMonthlyLimit() {
        return monthlyRequestLimit != null && monthlyRequestLimit > 0;
    }

    public boolean isUnlimited() {
        return !hasDailyLimit() && !hasMonthlyLimit();
    }

    @Override
    public String toString() {
        return "ClientNetworkAccess{" +
                "id=" + id +
                ", clientApplication=" + (clientApplication != null ? clientApplication.getName() : "null") +
                ", neuralNetwork=" + (neuralNetwork != null ? neuralNetwork.getName() : "null") +
                ", dailyRequestLimit=" + dailyRequestLimit +
                ", monthlyRequestLimit=" + monthlyRequestLimit +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
