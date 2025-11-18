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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    
    private final NetworkManagementService networkService;
    private final ClientManagementService clientService;
    private final RequestLogRepository requestLogRepository;
    private final com.example.integration.repository.NeuralNetworkRepository neuralNetworkRepository;
    private final com.example.integration.repository.ClientApplicationRepository clientApplicationRepository;
    private final com.example.integration.repository.PaymentHistoryRepository paymentHistoryRepository;
    
    public AdminController(
        NetworkManagementService networkService,
        ClientManagementService clientService,
        RequestLogRepository requestLogRepository,
        com.example.integration.repository.NeuralNetworkRepository neuralNetworkRepository,
        com.example.integration.repository.ClientApplicationRepository clientApplicationRepository,
        com.example.integration.repository.PaymentHistoryRepository paymentHistoryRepository
    ) {
        this.networkService = networkService;
        this.clientService = clientService;
        this.requestLogRepository = requestLogRepository;
        this.neuralNetworkRepository = neuralNetworkRepository;
        this.clientApplicationRepository = clientApplicationRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
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
    @Operation(summary = "Delete client application")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID id) {
        log.info("🗑️ [Admin] Запрос на удаление клиента с ID: {}", id);
        
        try {
            clientService.deleteClient(id);
            log.info("✅ [Admin] Клиент с ID {} успешно удален", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("❌ [Admin] Ошибка удаления клиента {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ [Admin] Неожиданная ошибка при удалении клиента {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ==================== Request Logs ====================
    
    @GetMapping({"/logs", "/request-logs", "/rl"})
    @Operation(summary = "Get request logs", description = "Get paginated list of request logs")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Page<com.example.integration.dto.RequestLogDTO>> getLogs(
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

        Page<com.example.integration.dto.RequestLogDTO> dtoPage = logs.map(log -> {
            var nn = log.getNeuralNetwork();
            var client = log.getClientApp();
            String externalUserId = (log.getExternalUser() != null) ? String.valueOf(log.getExternalUser().getId()) : null;
            String prompt = (log.getRequestPayload() != null) ? log.getRequestPayload().toString() : null;
            String response = (log.getResponsePayload() != null) ? log.getResponsePayload().toString() : null;
            boolean success = "success".equalsIgnoreCase(log.getStatus());
            return new com.example.integration.dto.RequestLogDTO(
                log.getId(),
                externalUserId,
                nn != null ? nn.getId() : null,
                nn != null ? nn.getDisplayName() : null,
                client != null ? client.getId() : null,
                client != null ? client.getName() : null,
                log.getRequestType(),
                prompt,
                response,
                success,
                log.getErrorMessage(),
                log.getTokensUsed(),
                log.getCreatedAt()
            );
        });

        return ResponseEntity.ok(dtoPage);
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
        
        // Получаем все логи для детального анализа
        List<RequestLog> allLogs = requestLogRepository.findAll();
        
        // Статистика по нейросетям (запросы)
        java.util.Map<String, Long> requestsByNetwork = allLogs.stream()
            .filter(log -> log.getNeuralNetwork() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                log -> log.getNeuralNetwork().getDisplayName(),
                java.util.stream.Collectors.counting()
            ));
        
        // Статистика по нейросетям (токены)
        java.util.Map<String, Long> tokensByNetwork = allLogs.stream()
            .filter(log -> log.getNeuralNetwork() != null && log.getTokensUsed() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                log -> log.getNeuralNetwork().getDisplayName(),
                java.util.stream.Collectors.summingLong(log -> log.getTokensUsed() != null ? log.getTokensUsed() : 0L)
            ));
        
        // Статистика по нейросетям (стоимость)
        java.util.Map<String, java.math.BigDecimal> costByNetwork = new java.util.HashMap<>();
        java.math.BigDecimal totalCostRub = java.math.BigDecimal.ZERO;
        
        for (RequestLog log : allLogs) {
            if (log.getNeuralNetwork() != null && log.getTokensUsed() != null && log.getTokensUsed() > 0) {
                String networkName = log.getNeuralNetwork().getDisplayName();
                java.math.BigDecimal costPerToken = log.getNeuralNetwork().getCostPerTokenRub();
                if (costPerToken != null && costPerToken.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    java.math.BigDecimal cost = costPerToken.multiply(java.math.BigDecimal.valueOf(log.getTokensUsed()));
                    costByNetwork.merge(networkName, cost, java.math.BigDecimal::add);
                    totalCostRub = totalCostRub.add(cost);
                }
            }
        }
        
        // Статистика по клиентам (запросы)
        java.util.Map<String, Long> requestsByClient = allLogs.stream()
            .filter(log -> log.getClientApp() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                log -> log.getClientApp().getName(),
                java.util.stream.Collectors.counting()
            ));
        
        // Статистика по клиентам (токены)
        java.util.Map<String, Long> tokensByClient = allLogs.stream()
            .filter(log -> log.getClientApp() != null && log.getTokensUsed() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                log -> log.getClientApp().getName(),
                java.util.stream.Collectors.summingLong(log -> log.getTokensUsed() != null ? log.getTokensUsed() : 0L)
            ));
        
        // Статистика по клиентам (стоимость)
        java.util.Map<String, java.math.BigDecimal> costByClient = new java.util.HashMap<>();
        for (RequestLog log : allLogs) {
            if (log.getClientApp() != null && log.getNeuralNetwork() != null && 
                log.getTokensUsed() != null && log.getTokensUsed() > 0) {
                String clientName = log.getClientApp().getName();
                java.math.BigDecimal costPerToken = log.getNeuralNetwork().getCostPerTokenRub();
                if (costPerToken != null && costPerToken.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    java.math.BigDecimal cost = costPerToken.multiply(java.math.BigDecimal.valueOf(log.getTokensUsed()));
                    costByClient.merge(clientName, cost, java.math.BigDecimal::add);
                }
            }
        }
        
        // Детальная статистика по нейросетям
        java.util.List<NetworkStatsDetailDto> networkDetails = new java.util.ArrayList<>();
        java.util.Map<java.util.UUID, java.util.List<RequestLog>> logsByNetworkId = allLogs.stream()
            .filter(log -> log.getNeuralNetwork() != null)
            .collect(java.util.stream.Collectors.groupingBy(log -> log.getNeuralNetwork().getId()));
        
        for (java.util.Map.Entry<java.util.UUID, java.util.List<RequestLog>> entry : logsByNetworkId.entrySet()) {
            java.util.UUID networkId = entry.getKey();
            java.util.List<RequestLog> networkLogs = entry.getValue();
            
            com.example.integration.model.NeuralNetwork network = neuralNetworkRepository.findById(networkId).orElse(null);
            if (network == null) continue;
            
            long networkRequests = networkLogs.size();
            long networkSuccessful = networkLogs.stream().filter(log -> "success".equals(log.getStatus())).count();
            long networkFailed = networkLogs.stream().filter(log -> "failed".equals(log.getStatus())).count();
            long networkTokens = networkLogs.stream()
                .filter(log -> log.getTokensUsed() != null)
                .mapToLong(RequestLog::getTokensUsed)
                .sum();
            
            java.math.BigDecimal networkCost = java.math.BigDecimal.ZERO;
            if (network.getCostPerTokenRub() != null && network.getCostPerTokenRub().compareTo(java.math.BigDecimal.ZERO) > 0) {
                networkCost = network.getCostPerTokenRub().multiply(java.math.BigDecimal.valueOf(networkTokens));
            }
            
            // Статистика по клиентам для этой нейросети
            java.util.Map<String, Long> requestsByClientForNetwork = networkLogs.stream()
                .filter(log -> log.getClientApp() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    log -> log.getClientApp().getName(),
                    java.util.stream.Collectors.counting()
                ));
            
            java.util.Map<String, Long> tokensByClientForNetwork = networkLogs.stream()
                .filter(log -> log.getClientApp() != null && log.getTokensUsed() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    log -> log.getClientApp().getName(),
                    java.util.stream.Collectors.summingLong(log -> log.getTokensUsed() != null ? log.getTokensUsed() : 0L)
                ));
            
            java.util.Map<String, java.math.BigDecimal> costByClientForNetwork = new java.util.HashMap<>();
            for (RequestLog log : networkLogs) {
                if (log.getClientApp() != null && log.getTokensUsed() != null && log.getTokensUsed() > 0) {
                    String clientName = log.getClientApp().getName();
                    java.math.BigDecimal costPerToken = network.getCostPerTokenRub();
                    if (costPerToken != null && costPerToken.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        java.math.BigDecimal cost = costPerToken.multiply(java.math.BigDecimal.valueOf(log.getTokensUsed()));
                        costByClientForNetwork.merge(clientName, cost, java.math.BigDecimal::add);
                    }
                }
            }
            
            NetworkStatsDetailDto networkDetail = new NetworkStatsDetailDto(
                networkId,
                network.getName(),
                network.getDisplayName(),
                network.getProvider(),
                networkRequests,
                networkSuccessful,
                networkFailed,
                networkTokens,
                networkCost,
                network.getCostPerTokenRub(),
                requestsByClientForNetwork,
                tokensByClientForNetwork,
                costByClientForNetwork
            );
            networkDetails.add(networkDetail);
        }
        
        // Детальная статистика по клиентам
        java.util.List<ClientStatsDetailDto> clientDetails = new java.util.ArrayList<>();
        java.util.Map<java.util.UUID, java.util.List<RequestLog>> logsByClientId = allLogs.stream()
            .filter(log -> log.getClientApp() != null)
            .collect(java.util.stream.Collectors.groupingBy(log -> log.getClientApp().getId()));
        
        for (java.util.Map.Entry<java.util.UUID, java.util.List<RequestLog>> entry : logsByClientId.entrySet()) {
            java.util.UUID clientId = entry.getKey();
            java.util.List<RequestLog> clientLogs = entry.getValue();
            
            com.example.integration.model.ClientApplication client = clientApplicationRepository.findById(clientId).orElse(null);
            if (client == null) continue;
            
            long clientRequests = clientLogs.size();
            long clientSuccessful = clientLogs.stream().filter(log -> "success".equals(log.getStatus())).count();
            long clientFailed = clientLogs.stream().filter(log -> "failed".equals(log.getStatus())).count();
            long clientTokens = clientLogs.stream()
                .filter(log -> log.getTokensUsed() != null)
                .mapToLong(RequestLog::getTokensUsed)
                .sum();
            
            java.math.BigDecimal clientCost = java.math.BigDecimal.ZERO;
            
            // Статистика по нейросетям для этого клиента
            java.util.Map<String, Long> requestsByNetworkForClient = clientLogs.stream()
                .filter(log -> log.getNeuralNetwork() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    log -> log.getNeuralNetwork().getDisplayName(),
                    java.util.stream.Collectors.counting()
                ));
            
            java.util.Map<String, Long> tokensByNetworkForClient = clientLogs.stream()
                .filter(log -> log.getNeuralNetwork() != null && log.getTokensUsed() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    log -> log.getNeuralNetwork().getDisplayName(),
                    java.util.stream.Collectors.summingLong(log -> log.getTokensUsed() != null ? log.getTokensUsed() : 0L)
                ));
            
            java.util.Map<String, java.math.BigDecimal> costByNetworkForClient = new java.util.HashMap<>();
            for (RequestLog log : clientLogs) {
                if (log.getNeuralNetwork() != null && log.getTokensUsed() != null && log.getTokensUsed() > 0) {
                    String networkName = log.getNeuralNetwork().getDisplayName();
                    java.math.BigDecimal costPerToken = log.getNeuralNetwork().getCostPerTokenRub();
                    if (costPerToken != null && costPerToken.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        java.math.BigDecimal cost = costPerToken.multiply(java.math.BigDecimal.valueOf(log.getTokensUsed()));
                        costByNetworkForClient.merge(networkName, cost, java.math.BigDecimal::add);
                        clientCost = clientCost.add(cost);
                    }
                }
            }
            
            ClientStatsDetailDto clientDetail = new ClientStatsDetailDto(
                clientId,
                client.getName(),
                clientRequests,
                clientSuccessful,
                clientFailed,
                clientTokens,
                clientCost,
                requestsByNetworkForClient,
                tokensByNetworkForClient,
                costByNetworkForClient
            );
            clientDetails.add(clientDetail);
        }
        
        AdminStatsDTO stats = new AdminStatsDTO(
            totalRequests,
            successfulRequests,
            failedRequests,
            totalTokensUsed,
            totalCostRub,
            requestsByNetwork,
            requestsByClient,
            tokensByNetwork,
            costByNetwork,
            tokensByClient,
            costByClient,
            networkDetails,
            clientDetails
        );
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Получить статистику оплат от пользователей
     */
    @GetMapping("/payments/stats")
    @Operation(summary = "Get payment statistics", description = "Get all payments from users with dates and amounts")
    public ResponseEntity<List<com.example.integration.dto.PaymentStatsDto>> getPaymentStats() {
        log.info("Получение статистики оплат");
        
        List<com.example.integration.model.PaymentHistory> payments = paymentHistoryRepository.findAll();
        
        List<com.example.integration.dto.PaymentStatsDto> stats = payments.stream()
                .map(payment -> {
                    com.example.integration.dto.PaymentStatsDto dto = new com.example.integration.dto.PaymentStatsDto();
                    dto.setPaymentId(payment.getId());
                    dto.setUserId(payment.getUserAccount().getId());
                    dto.setUserEmail(payment.getUserAccount().getEmail());
                    dto.setUserFullName(payment.getUserAccount().getFullName());
                    dto.setPlanName(payment.getSubscriptionPlan().getName());
                    dto.setPlanDisplayName(payment.getSubscriptionPlan().getDisplayName());
                    dto.setAmount(payment.getAmount());
                    dto.setCurrency(payment.getCurrency());
                    dto.setStatus(payment.getStatus().name());
                    dto.setCreatedAt(payment.getCreatedAt());
                    dto.setCompletedAt(payment.getCompletedAt());
                    dto.setTransactionId(payment.getTransactionId());
                    return dto;
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(stats);
    }
}

