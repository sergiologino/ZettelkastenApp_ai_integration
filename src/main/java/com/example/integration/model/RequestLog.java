package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "request_logs")
@Data
@NoArgsConstructor
public class RequestLog {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_app_id", nullable = false)
    private ClientApplication clientApp;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_user_id")
    private ExternalUser externalUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neural_network_id")
    private NeuralNetwork neuralNetwork;
    
    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType; // chat, transcription, etc.
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requestPayload;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private Map<String, Object> responsePayload;
    
    @Column(nullable = false, length = 50)
    private String status; // pending, success, failed, rate_limited
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;
    
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    public void markCompleted(String status, Map<String, Object> response, Integer executionTime, Integer tokens) {
        this.status = status;
        this.responsePayload = response;
        this.executionTimeMs = executionTime;
        this.tokensUsed = tokens;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markFailed(String errorMessage, Integer executionTime) {
        this.status = "failed";
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTime;
        this.completedAt = LocalDateTime.now();
    }
}

