package com.example.integration.controller;

import com.example.integration.dto.*;
import com.example.integration.model.RequestLog;
import com.example.integration.repository.RequestLogRepository;
import com.example.integration.service.ClientManagementService;
import com.example.integration.service.NetworkManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API для администрирования AI-сервиса
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin API", description = "API for AI service administration")
@SecurityRequirement(name = "Bearer")
public class AdminController {
    
    private final NetworkManagementService networkService;
    private final ClientManagementService clientService;
    private final RequestLogRepository requestLogRepository;
    
    public AdminController(
        NetworkManagementService networkService,
        ClientManagementService clientService,
        RequestLogRepository requestLogRepository
    ) {
        this.networkService = networkService;
        this.clientService = clientService;
        this.requestLogRepository = requestLogRepository;
    }
    
    // ==================== Neural Networks ====================
    
    @GetMapping("/networks")
    @Operation(summary = "Get all networks", description = "Get list of all neural networks")
    public ResponseEntity<List<NetworkDTO>> getAllNetworks() {
        return ResponseEntity.ok(networkService.getAllNetworks());
    }
    
    @GetMapping("/networks/{id}")
    @Operation(summary = "Get network by ID")
    public ResponseEntity<NetworkDTO> getNetwork(@PathVariable UUID id) {
        return ResponseEntity.ok(networkService.getNetwork(id));
    }
    
    @PostMapping("/networks")
    @Operation(
        summary = "Create new network",
        description = "Создать новую нейросеть",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Пример создания OpenAI GPT-4",
            content = @io.swagger.v3.oas.annotations.media.Content(
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = """
                        {
                          "name": "openai-gpt4-turbo",
                          "displayName": "OpenAI GPT-4 Turbo",
                          "provider": "openai",
                          "networkType": "chat",
                          "apiUrl": "https://api.openai.com/v1",
                          "apiKey": "sk-proj-your-api-key-here",
                          "modelName": "gpt-4-turbo",
                          "isActive": true,
                          "isFree": false,
                          "priority": 5,
                          "timeoutSeconds": 60,
                          "maxRetries": 3,
                          "requestMapping": {},
                          "responseMapping": {}
                        }
                        """
                )
            )
        )
    )
    public ResponseEntity<NetworkDTO> createNetwork(@Valid @RequestBody NetworkCreateRequest request) {
        return ResponseEntity.ok(networkService.createNetwork(request));
    }
    
    @PutMapping("/networks/{id}")
    @Operation(summary = "Update network")
    public ResponseEntity<NetworkDTO> updateNetwork(
        @PathVariable UUID id,
        @Valid @RequestBody NetworkCreateRequest request
    ) {
        return ResponseEntity.ok(networkService.updateNetwork(id, request));
    }
    
    @DeleteMapping("/networks/{id}")
    @Operation(summary = "Delete network")
    public ResponseEntity<Void> deleteNetwork(@PathVariable UUID id) {
        networkService.deleteNetwork(id);
        return ResponseEntity.ok().build();
    }
    
    // ==================== Client Applications ====================
    
    @GetMapping("/clients")
    @Operation(summary = "Get all clients", description = "Get list of all client applications")
    public ResponseEntity<List<ClientAppDTO>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }
    
    @PostMapping("/clients")
    @Operation(summary = "Create new client application")
    public ResponseEntity<ClientAppDTO> createClient(@Valid @RequestBody ClientAppCreateRequest request) {
        return ResponseEntity.ok(clientService.createClient(request));
    }
    
    @PutMapping("/clients/{id}")
    @Operation(summary = "Update client application")
    public ResponseEntity<ClientAppDTO> updateClient(
        @PathVariable UUID id,
        @Valid @RequestBody ClientAppCreateRequest request
    ) {
        return ResponseEntity.ok(clientService.updateClient(id, request));
    }
    
    @PostMapping("/clients/{id}/regenerate-key")
    @Operation(summary = "Regenerate API key for client")
    public ResponseEntity<ClientAppDTO> regenerateApiKey(@PathVariable UUID id) {
        return ResponseEntity.ok(clientService.regenerateApiKey(id));
    }
    
    @DeleteMapping("/clients/{id}")
    @Operation(summary = "Deactivate client application")
    public ResponseEntity<Void> deactivateClient(@PathVariable UUID id) {
        clientService.deactivateClient(id);
        return ResponseEntity.ok().build();
    }
    
    // ==================== Request Logs ====================
    
    @GetMapping("/logs")
    @Operation(summary = "Get request logs", description = "Get paginated list of request logs")
    public ResponseEntity<Page<RequestLog>> getLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<RequestLog> logs;
        if (status != null && !status.isEmpty()) {
            logs = requestLogRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            logs = requestLogRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/logs/{id}")
    @Operation(summary = "Get log by ID")
    public ResponseEntity<RequestLog> getLog(@PathVariable UUID id) {
        return ResponseEntity.ok(
            requestLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log not found"))
        );
    }
    
    // ==================== Statistics ====================
    
    @GetMapping("/stats")
    @Operation(summary = "Get service statistics")
    public ResponseEntity<AdminStatsDTO> getStats() {
        // Сбор общей статистики из логов
        long totalRequests = requestLogRepository.count();
        long successfulRequests = requestLogRepository.countByStatus("success");
        long failedRequests = requestLogRepository.countByStatus("failed");
        
        // Подсчет токенов (может быть null, поэтому используем 0 как fallback)
        Long totalTokens = requestLogRepository.sumTokensUsed();
        long totalTokensUsed = (totalTokens != null) ? totalTokens : 0L;
        
        // Статистика по нейросетям
        List<RequestLog> allLogs = requestLogRepository.findAll();
        java.util.Map<String, Long> requestsByNetwork = allLogs.stream()
            .filter(log -> log.getNeuralNetwork() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                log -> log.getNeuralNetwork().getDisplayName(),
                java.util.stream.Collectors.counting()
            ));
        
        // Статистика по клиентам
        java.util.Map<String, Long> requestsByClient = allLogs.stream()
            .filter(log -> log.getClientApp() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                log -> log.getClientApp().getName(),
                java.util.stream.Collectors.counting()
            ));
        
        AdminStatsDTO stats = new AdminStatsDTO(
            totalRequests,
            successfulRequests,
            failedRequests,
            totalTokensUsed,
            requestsByNetwork,
            requestsByClient
        );
        
        return ResponseEntity.ok(stats);
    }
}

