package com.example.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Запрос на публикацию поста во внешнюю социальную платформу")
public class SocialPostRequestDTO {

    @NotBlank
    @Schema(description = "ID пользователя во внешней системе клиента", example = "user123")
    private String userId;

    @NotBlank
    @Schema(description = "Платформа публикации", allowableValues = {"telegram", "facebook", "x"}, example = "telegram")
    private String platform;

    @NotBlank
    @Schema(description = "Текст поста", example = "Новый пост из AI Integration Service")
    private String text;

    @NotEmpty
    @Schema(description = "Секреты и идентификаторы платформы. Не сохраняются в логах.")
    private Map<String, String> credentials;

    @Schema(description = "Дополнительные параметры платформы: parseMode, link, replyToTweetId и т.д.")
    private Map<String, Object> options;
}
