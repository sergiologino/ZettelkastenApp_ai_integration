package com.example.integration.controller;

import com.example.integration.model.AnalysisRequest;
import com.example.integration.model.AnalysisResponse;
import com.example.integration.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @Operation(summary = "Анализ заметки",
            description = "Обрабатывает текст заметки или файл и возвращает аннотацию и теги.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный анализ",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка на стороне сервера")
    })
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@RequestBody AnalysisRequest request) {
        AnalysisResponse response = integrationService.analyze(request);
        return ResponseEntity.ok(response);
    }
}
