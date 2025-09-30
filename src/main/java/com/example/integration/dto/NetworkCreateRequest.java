package com.example.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
@Schema(description = "Данные для создания/обновления нейросети")
public class NetworkCreateRequest {
    
    @Schema(description = "Уникальное имя нейросети", example = "openai-gpt4", required = true)
    @NotBlank(message = "Name is required")
    private String name;
    
    @Schema(description = "Отображаемое имя", example = "OpenAI GPT-4", required = true)
    @NotBlank(message = "Display name is required")
    private String displayName;
    
    @Schema(description = "Провайдер", example = "openai", allowableValues = {"openai", "yandex", "anthropic", "mistral", "sber", "whisper"}, required = true)
    @NotBlank(message = "Provider is required")
    private String provider;
    
    @Schema(description = "Тип нейросети", example = "chat", allowableValues = {"chat", "transcription", "embedding"}, required = true)
    @NotBlank(message = "Network type is required")
    private String networkType;
    
    @Schema(description = "URL API нейросети", example = "https://api.openai.com/v1", required = true)
    @NotBlank(message = "API URL is required")
    private String apiUrl;
    
    @Schema(description = "API ключ (будет зашифрован)", example = "sk-proj-your-openai-api-key-here")
    private String apiKey;
    
    @Schema(description = "Название модели", example = "gpt-4")
    private String modelName;
    
    @Schema(description = "Активна ли нейросеть", example = "true", required = true)
    @NotNull(message = "isActive flag is required")
    private Boolean isActive;
    
    @Schema(description = "Бесплатная ли нейросеть", example = "false", required = true)
    @NotNull(message = "isFree flag is required")
    private Boolean isFree;
    
    @Schema(description = "Приоритет (меньше = выше)", example = "10")
    private Integer priority = 100;
    
    @Schema(description = "Таймаут в секундах", example = "60")
    private Integer timeoutSeconds = 60;
    
    @Schema(description = "Максимум повторных попыток", example = "3")
    private Integer maxRetries = 3;
    
    @Schema(description = "Маппинг полей запроса (JSON)", example = "{}")
    private Map<String, Object> requestMapping;
    
    @Schema(description = "Маппинг полей ответа (JSON)", example = "{}")
    private Map<String, Object> responseMapping;
}

