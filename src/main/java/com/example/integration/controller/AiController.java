package com.example.integration.controller;

import com.example.integration.dto.AiRequestDTO;
import com.example.integration.dto.AiResponseDTO;
import com.example.integration.dto.AvailableNetworkDTO;
import com.example.integration.model.ClientApplication;
import com.example.integration.service.AiOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API для клиентских приложений - обработка AI-запросов
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI API", description = "API for client applications to process AI requests")
@SecurityRequirement(name = "X-API-Key")
public class AiController {
    
    private final AiOrchestrationService aiOrchestrationService;
    
    public AiController(AiOrchestrationService aiOrchestrationService) {
        this.aiOrchestrationService = aiOrchestrationService;
    }
    
    /**
     * Отправить запрос в AI
     */
    @PostMapping("/process")
    @Operation(
        summary = "Process AI request", 
        description = "Отправить запрос в нейросеть и получить ответ. Требуется X-API-Key в заголовке.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Пример запроса для chat",
            content = @io.swagger.v3.oas.annotations.media.Content(
                examples = {
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Chat request",
                        value = """
                            {
                              "userId": "user123",
                              "networkName": "openai-gpt4",
                              "requestType": "chat",
                              "payload": {
                                "messages": [
                                  {
                                    "role": "user",
                                    "content": "Привет! Расскажи интересный факт о космосе."
                                  }
                                ]
                              }
                            }
                            """
                    ),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Auto-select network",
                        value = """
                            {
                              "userId": "user456",
                              "requestType": "chat",
                              "payload": {
                                "messages": [
                                  {
                                    "role": "system",
                                    "content": "Ты полезный ассистент."
                                  },
                                  {
                                    "role": "user",
                                    "content": "Как погода сегодня?"
                                  }
                                ]
                              }
                            }
                            """
                    )
                }
            )
        )
    )
    public ResponseEntity<AiResponseDTO> processRequest(
        @Valid @RequestBody AiRequestDTO request,
        Authentication authentication
    ) {
        ClientApplication clientApp = (ClientApplication) authentication.getPrincipal();
        AiResponseDTO response = aiOrchestrationService.processRequest(clientApp, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Получить доступные нейросети для клиента
     */
    @GetMapping("/networks/available")
    @Operation(
        summary = "Get available networks", 
        description = "Получить список доступных нейросетей для клиента с учетом лимитов доступа"
    )
    public ResponseEntity<List<AvailableNetworkDTO>> getAvailableNetworks() {
        // TODO: Добавить авторизацию через X-API-Key
        List<AvailableNetworkDTO> networks = aiOrchestrationService.getAllAvailableNetworks();
        return ResponseEntity.ok(networks);
    }
    
    /**
     * Проверить доступность конкретной нейросети
     */
    @GetMapping("/networks/{networkId}/available")
    @Operation(summary = "Check network availability", description = "Проверить доступность конкретной нейросети")
    public ResponseEntity<Map<String, Object>> checkNetworkAvailability(@PathVariable String networkId) {
        // TODO: Добавить авторизацию через X-API-Key
        boolean isAvailable = aiOrchestrationService.isNetworkAvailable(networkId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("networkId", networkId);
        response.put("available", isAvailable);
        
        if (isAvailable) {
            // Получаем информацию о лимитах
            Map<String, Object> limits = aiOrchestrationService.getNetworkLimits(networkId);
            response.put("limits", limits);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Получить лимиты для нейросети
     */
    @GetMapping("/networks/{networkId}/limits")
    @Operation(summary = "Get network limits", description = "Получить информацию о лимитах для нейросети")
    public ResponseEntity<Map<String, Object>> getNetworkLimits(@PathVariable String networkId) {
        // TODO: Добавить авторизацию через X-API-Key
        Map<String, Object> limits = aiOrchestrationService.getNetworkLimits(networkId);
        return ResponseEntity.ok(limits);
    }
    
    /**
     * Проверка доступности сервиса
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if AI service is available")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Integration Service is running");
    }
}

