package com.example.integration.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AnalysisResponse {

    @Schema(description = "Аннотация, возвращенная нейросетью", example = "Результат анализа текста")
    private String annotation;

    @Schema(description = "Список тегов, связанных с заметкой", example = "[\"тег1\", \"тег2\"]")
    private String[] tags;
}
