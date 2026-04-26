package com.example.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
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

    @Schema(description = "Текст поста или caption для медиа", example = "Новый пост из AI Integration Service")
    private String text;

    @Valid
    @Schema(description = "Вложения поста: документы, изображения, видео. Содержимое не сохраняется в логах.")
    private List<Attachment> attachments;

    @NotEmpty
    @Schema(description = "Секреты и идентификаторы платформы. Не сохраняются в логах.")
    private Map<String, String> credentials;

    @Schema(description = "Дополнительные параметры платформы: parseMode, link, replyToTweetId и т.д.")
    private Map<String, Object> options;

    @Data
    @Schema(description = "Файл или медиа для публикации")
    public static class Attachment {

        @NotBlank
        @Schema(description = "Тип вложения", allowableValues = {"image", "photo", "video", "document", "file"}, example = "image")
        private String type;

        @Schema(description = "Имя файла для multipart-загрузки", example = "photo.jpg")
        private String fileName;

        @Schema(description = "MIME-тип файла", example = "image/jpeg")
        private String contentType;

        @Schema(description = "Base64 содержимое файла. Можно передавать data URL; префикс будет отброшен.")
        private String base64;

        @Schema(description = "Публичный URL файла, если провайдер поддерживает загрузку по URL")
        private String url;

        @Schema(description = "Caption для конкретного файла. Если не указан, для первого вложения используется text.")
        private String caption;
    }
}
