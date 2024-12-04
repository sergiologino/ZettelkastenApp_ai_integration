package com.example.integration.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AnalysisRequest {

    @Schema(description = "Текст заметки для анализа", example = "Пример текста заметки")
    private String content;

    @Schema(description = "URL файла для анализа", example = "http://example.com/file.pdf")
    private String fileUrl;

    @Schema(description = "Тип файла (image, pdf, audio)", example = "pdf")
    private String fileType;

    @NotEmpty
    @Schema(description = "Целевая нейросеть для анализа", example = "OpenAI")
    private String neuralNetwork;
}
