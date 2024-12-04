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

    public String getContent() {
        return content;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public @NotEmpty String getNeuralNetwork() {
        return neuralNetwork;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setNeuralNetwork(@NotEmpty String neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }
}
