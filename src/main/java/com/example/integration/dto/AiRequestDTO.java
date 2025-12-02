package com.example.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Data
@Schema(description = "AI запрос от клиентского приложения")
public class AiRequestDTO {
    
    @Schema(
        description = "ID пользователя в клиентском приложении", 
        example = "user123",
        required = true
    )
    private String userId;
    
    @Schema(
        description = "Название нейросети (null для автовыбора)", 
        example = "openai-gpt4",
        nullable = true
    )
    private String networkName;
    
    @Schema(
        description = "Тип запроса", 
        example = "chat",
        allowableValues = {"chat", "transcription", "embedding", "image_generation", "video_generation"}
    )
    private String requestType;
    
    @Schema(
        description = "Данные запроса (формат зависит от типа запроса)",
        example = "{\"messages\": [{\"role\": \"user\", \"content\": \"Привет, как дела?\"}]}"
    )
    private Map<String, Object> payload;
    
    @Schema(
        description = "Дополнительные метаданные",
        example = "{\"source\": \"web\", \"version\": \"1.0\"}"
    )
    private Map<String, String> metadata;
}

