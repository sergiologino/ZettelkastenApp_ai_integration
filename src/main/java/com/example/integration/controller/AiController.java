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
        description = "Получить список доступных нейросетей для клиента с учетом лимитов доступа. Требуется X-API-Key в заголовке.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", 
                description = "Список доступных нейросетей",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Available networks response",
                        value = """
                            [
                              {
                                "id": "123e4567-e89b-12d3-a456-426614174000",
                                "name": "openai-whisper",
                                "displayName": "OpenAI Whisper",
                                "provider": "openai",
                                "networkType": "transcription",
                                "modelName": "whisper-1",
                                "isFree": false,
                                "priority": 10,
                                "remainingRequestsToday": 100,
                                "remainingRequestsMonth": 1000,
                                "hasLimits": true
                              },
                              {
                                "id": "123e4567-e89b-12d3-a456-426614174001",
                                "name": "yandex-speechkit",
                                "displayName": "Yandex SpeechKit",
                                "provider": "yandex",
                                "networkType": "transcription",
                                "modelName": "general",
                                "isFree": true,
                                "priority": 5,
                                "remainingRequestsToday": 50,
                                "remainingRequestsMonth": 500,
                                "hasLimits": true
                              }
                            ]
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401", 
                description = "Неверный или отсутствующий API ключ"
            )
        }
    )
    public ResponseEntity<List<AvailableNetworkDTO>> getAvailableNetworks(Authentication authentication) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiController.class);
        log.info("🔵 [AiController] ===== ЗАПРОС /api/ai/networks/available =====");
        
        // ✅ ИСПРАВЛЕНИЕ: Получаем клиента из SecurityContext (установлен ApiKeyAuthFilter)
        if (authentication == null || !(authentication.getPrincipal() instanceof ClientApplication)) {
            log.warn("⚠️ [AiController] Аутентификация не пройдена или клиент не найден");
            log.warn("   Authentication: {}", authentication != null ? "есть" : "null");
            if (authentication != null && authentication.getPrincipal() != null) {
                log.warn("   Principal type: {}", authentication.getPrincipal().getClass().getName());
            }
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .build();
        }
        
        ClientApplication clientApp = (ClientApplication) authentication.getPrincipal();
        log.info("✅ [AiController] Клиент найден: {} (ID: {})", clientApp.getName(), clientApp.getId());
        
        // ✅ Используем метод, который возвращает только доступные сети для этого клиента
        log.info("🔍 [AiController] Вызываем getAvailableNetworksForClient...");
        List<AvailableNetworkDTO> networks = aiOrchestrationService.getAvailableNetworksForClient(clientApp);
        log.info("✅ [AiController] Получено {} доступных нейросетей для клиента {}", networks.size(), clientApp.getName());
        
        return ResponseEntity.ok(networks);
    }
    
    /**
     * Проверить доступность конкретной нейросети
     */
    @GetMapping("/networks/{networkId}/available")
    @Operation(
        summary = "Check network availability", 
        description = "Проверить доступность конкретной нейросети для клиента. Требуется X-API-Key в заголовке.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", 
                description = "Информация о доступности нейросети",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Network available",
                            value = """
                                {
                                  "networkId": "openai-whisper",
                                  "available": true,
                                  "limits": {
                                    "networkId": "openai-whisper",
                                    "networkName": "OpenAI Whisper",
                                    "isFree": false,
                                    "priority": 10,
                                    "remainingRequestsToday": 100,
                                    "remainingRequestsMonth": 1000,
                                    "hasLimits": true
                                  }
                                }
                                """
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Network unavailable",
                            value = """
                                {
                                  "networkId": "inactive-network",
                                  "available": false
                                }
                                """
                        )
                    }
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401", 
                description = "Неверный или отсутствующий API ключ"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404", 
                description = "Нейросеть не найдена"
            )
        }
    )
    public ResponseEntity<Map<String, Object>> checkNetworkAvailability(@PathVariable String networkId,
                                                                        Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("networkId", networkId);

        if (authentication != null && authentication.getPrincipal() instanceof ClientApplication clientApp) {
            boolean availableForClient = aiOrchestrationService.isNetworkAvailableForClient(clientApp, networkId);
            response.put("available", availableForClient);
            if (availableForClient) {
                response.put("limits", aiOrchestrationService.getClientNetworkLimits(clientApp, networkId));
            }
        } else {
            // публичная проверка, без учёта конкретного клиента
            boolean isAvailable = aiOrchestrationService.isNetworkAvailable(networkId);
            response.put("available", isAvailable);
            if (isAvailable) {
                response.put("limits", aiOrchestrationService.getNetworkLimits(networkId));
            }
        }

        return ResponseEntity.ok(response);
    }
    
    /**
     * Получить лимиты для нейросети
     */
    @GetMapping("/networks/{networkId}/limits")
    @Operation(
        summary = "Get network limits", 
        description = "Получить подробную информацию о лимитах и возможностях нейросети. Требуется X-API-Key в заголовке.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", 
                description = "Информация о лимитах нейросети",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Network limits",
                            value = """
                                {
                                  "networkId": "openai-whisper",
                                  "networkName": "OpenAI Whisper",
                                  "isFree": false,
                                  "priority": 10,
                                  "remainingRequestsToday": 100,
                                  "remainingRequestsMonth": 1000,
                                  "hasLimits": true
                                }
                                """
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Free network limits",
                            value = """
                                {
                                  "networkId": "yandex-speechkit",
                                  "networkName": "Yandex SpeechKit",
                                  "isFree": true,
                                  "priority": 5,
                                  "remainingRequestsToday": 50,
                                  "remainingRequestsMonth": 500,
                                  "hasLimits": true
                                }
                                """
                        )
                    }
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401", 
                description = "Неверный или отсутствующий API ключ"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404", 
                description = "Нейросеть не найдена",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Network not found",
                        value = """
                            {
                              "error": "Network not found"
                            }
                            """
                    )
                )
            )
        }
    )
    public ResponseEntity<Map<String, Object>> getNetworkLimits(@PathVariable String networkId,
                                                                Authentication authentication) {
        Map<String, Object> limits;
        if (authentication != null && authentication.getPrincipal() instanceof ClientApplication clientApp) {
            limits = aiOrchestrationService.getClientNetworkLimits(clientApp, networkId);
        } else {
            limits = aiOrchestrationService.getNetworkLimits(networkId);
        }
        return ResponseEntity.ok(limits);
    }
    
    /**
     * Проверка доступности сервиса
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check", 
        description = "Проверить доступность AI Integration Service. Не требует авторизации.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", 
                description = "Сервис доступен",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "text/plain",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Service running",
                        value = "AI Integration Service is running"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500", 
                description = "Сервис недоступен"
            )
        }
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Integration Service is running");
    }
}

