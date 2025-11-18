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
@Table(name = "neural_networks")
@Data
@NoArgsConstructor
public class NeuralNetwork {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    
    @Column(nullable = false, length = 50)
    private String provider; // openai, yandex, anthropic, mistral, sber
    
    @Column(name = "network_type", nullable = false, length = 50)
    private String networkType; // chat, transcription, embedding
    
    @Column(name = "api_url", nullable = false, columnDefinition = "TEXT")
    private String apiUrl;
    
    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;
    
    @Column(name = "model_name", length = 100)
    private String modelName; // gpt-4, claude-3, etc.
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_free", nullable = false)
    private Boolean isFree = false;
    
    @Column(nullable = false)
    private Integer priority = 100;
    
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 60;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_mapping", columnDefinition = "jsonb")
    private Map<String, Object> requestMapping;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_mapping", columnDefinition = "jsonb")
    private Map<String, Object> responseMapping;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "connection_instruction", columnDefinition = "TEXT")
    private String connectionInstruction;
    
    @Column(name = "cost_per_token_rub", precision = 19, scale = 8)
    private java.math.BigDecimal costPerTokenRub; // Себестоимость одного токена в рублях
    
    @Column(name = "words_per_token", precision = 10, scale = 4)
    private java.math.BigDecimal wordsPerToken; // Примерное количество слов в одном токене (для текстовых моделей)
    
    @Column(name = "seconds_per_token", precision = 10, scale = 4)
    private java.math.BigDecimal secondsPerToken; // Примерное количество секунд в одном токене (для транскрибации)
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

