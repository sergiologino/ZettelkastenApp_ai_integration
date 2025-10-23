package com.example.integration.service;

import com.example.integration.client.BaseNeuralClient;
import com.example.integration.client.NeuralClientFactory;
import com.example.integration.dto.AiRequestDTO;
import com.example.integration.dto.AiResponseDTO;
import com.example.integration.dto.AvailableNetworkDTO;
import com.example.integration.model.*;
import com.example.integration.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Главный сервис для обработки AI-запросов
 */
@Service
public class AiOrchestrationService {
    
    private static final Logger log = LoggerFactory.getLogger(AiOrchestrationService.class);
    
    private final NeuralClientFactory clientFactory;
    private final RateLimitService rateLimitService;
    private final NeuralNetworkRepository neuralNetworkRepository;
    private final ExternalUserRepository externalUserRepository;
    private final RequestLogRepository requestLogRepository;
    private final ClientApplicationRepository clientAppRepository;
    
    @Value("${ai.enable-fallback:true}")
    private boolean enableFallback;
    
    public AiOrchestrationService(
        NeuralClientFactory clientFactory,
        RateLimitService rateLimitService,
        NeuralNetworkRepository neuralNetworkRepository,
        ExternalUserRepository externalUserRepository,
        RequestLogRepository requestLogRepository,
        ClientApplicationRepository clientAppRepository
    ) {
        this.clientFactory = clientFactory;
        this.rateLimitService = rateLimitService;
        this.neuralNetworkRepository = neuralNetworkRepository;
        this.externalUserRepository = externalUserRepository;
        this.requestLogRepository = requestLogRepository;
        this.clientAppRepository = clientAppRepository;
    }
    
    /**
     * Обработать AI-запрос
     */
    @Transactional
    public AiResponseDTO processRequest(ClientApplication clientApp, AiRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        // 1. Получить или создать пользователя
        ExternalUser user = getOrCreateUser(clientApp, request.getUserId());
        
        // 2. Выбрать нейросеть
        NeuralNetwork network = selectNetwork(request.getNetworkName(), request.getRequestType(), user);
        
        // 3. Создать лог запроса
        RequestLog requestLog = createRequestLog(clientApp, user, network, request);
        
        try {
            // 4. Отправить запрос в нейросеть
            BaseNeuralClient client = clientFactory.getClient(network);
            Map<String, Object> response = client.sendRequest(network, request.getPayload());
            
            // 5. Извлечь количество токенов
            Integer tokensUsed = extractTokensFromResponse(response);
            
            // 6. Обновить счётчик использования
            rateLimitService.recordUsage(user, network, tokensUsed);
            
            // 7. Обновить лог
            int executionTime = (int) (System.currentTimeMillis() - startTime);
            requestLog.markCompleted("success", response, executionTime, tokensUsed);
            requestLogRepository.save(requestLog);
            
            // 8. Сформировать ответ
            return buildResponse(requestLog.getId().toString(), network, response, tokensUsed, executionTime, user);
            
        } catch (Exception e) {
            log.error("Error processing AI request", e);
            
            // Попытка fallback на бесплатную нейросеть
            if (enableFallback && e.getMessage() != null && e.getMessage().contains("rate limit")) {
                Optional<NeuralNetwork> fallbackNetwork = rateLimitService
                    .findFallbackNetwork(user, request.getRequestType());
                
                if (fallbackNetwork.isPresent()) {
                    log.info("Switching to fallback network: {}", fallbackNetwork.get().getName());
                    
                    AiRequestDTO fallbackRequest = new AiRequestDTO();
                    fallbackRequest.setUserId(request.getUserId());
                    fallbackRequest.setNetworkName(fallbackNetwork.get().getName());
                    fallbackRequest.setRequestType(request.getRequestType());
                    fallbackRequest.setPayload(request.getPayload());
                    
                    return processRequest(clientApp, fallbackRequest);
                }
            }
            
            // Если fallback не удался или не включен - возвращаем ошибку
            int executionTime = (int) (System.currentTimeMillis() - startTime);
            requestLog.markFailed(e.getMessage(), executionTime);
            requestLogRepository.save(requestLog);
            
            AiResponseDTO errorResponse = new AiResponseDTO();
            errorResponse.setRequestId(requestLog.getId().toString());
            errorResponse.setStatus("failed");
            errorResponse.setErrorMessage(e.getMessage());
            errorResponse.setExecutionTimeMs(executionTime);
            return errorResponse;
        }
    }
    
    private ExternalUser getOrCreateUser(ClientApplication clientApp, String externalUserId) {
        return externalUserRepository
            .findByClientAppAndExternalUserId(clientApp, externalUserId)
            .orElseGet(() -> {
                ExternalUser newUser = new ExternalUser();
                newUser.setClientApp(clientApp);
                newUser.setExternalUserId(externalUserId);
                newUser.setUserType("free_user"); // По умолчанию
                return externalUserRepository.save(newUser);
            });
    }
    
    private NeuralNetwork selectNetwork(String networkName, String requestType, ExternalUser user) {
        NeuralNetwork network;
        
        if (networkName != null && !networkName.isEmpty()) {
            // Пользователь указал конкретную нейросеть
            network = neuralNetworkRepository.findByName(networkName)
                .orElseThrow(() -> new IllegalArgumentException("Network not found: " + networkName));
        } else {
            // Автоматический выбор по типу запроса и приоритету
            network = neuralNetworkRepository.findByTypeOrderedByPriority(requestType)
                .stream()
                .filter(n -> n.getIsActive() && rateLimitService.isNetworkAvailable(user, n))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available network for type: " + requestType));
        }
        
        // Проверяем доступность
        if (!rateLimitService.isNetworkAvailable(user, network)) {
            throw new IllegalStateException("Rate limit exceeded for network: " + network.getName());
        }
        
        return network;
    }
    
    private RequestLog createRequestLog(
        ClientApplication clientApp, 
        ExternalUser user, 
        NeuralNetwork network, 
        AiRequestDTO request
    ) {
        RequestLog log = new RequestLog();
        log.setClientApp(clientApp);
        log.setExternalUser(user);
        log.setNeuralNetwork(network);
        log.setRequestType(request.getRequestType());
        log.setRequestPayload(request.getPayload());
        log.setStatus("pending");
        return requestLogRepository.save(log);
    }
    
    private Integer extractTokensFromResponse(Map<String, Object> response) {
        if (response.containsKey("usage")) {
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            if (usage.containsKey("total_tokens")) {
                return ((Number) usage.get("total_tokens")).intValue();
            }
        }
        return 0;
    }
    
    private AiResponseDTO buildResponse(
        String requestId,
        NeuralNetwork network,
        Map<String, Object> response,
        Integer tokensUsed,
        Integer executionTime,
        ExternalUser user
    ) {
        AiResponseDTO dto = new AiResponseDTO();
        dto.setRequestId(requestId);
        dto.setStatus("success");
        dto.setNetworkUsed(network.getName());
        dto.setResponse(response);
        dto.setExecutionTimeMs(executionTime);
        dto.setTokensUsed(tokensUsed);
        
        // Добавляем информацию о лимитах
        AiResponseDTO.UsageLimitInfo limitInfo = new AiResponseDTO.UsageLimitInfo();
        Integer remaining = rateLimitService.getRemainingRequests(user, network);
        
        if (remaining != null) {
            limitInfo.setRemaining(remaining);
            limitInfo.setUsed(tokensUsed);
            limitInfo.setPeriod("daily");
            // TODO: получить лимит из NetworkLimit
        }
        
        dto.setUsageLimitInfo(limitInfo);
        
        return dto;
    }
    
    /**
     * Получить все доступные нейросети (упрощенная версия)
     */
    public List<AvailableNetworkDTO> getAllAvailableNetworks() {
        // Получаем все активные нейросети
        List<NeuralNetwork> allNetworks = neuralNetworkRepository.findByIsActiveTrue();
        
        return allNetworks.stream()
            .map(this::convertToAvailableNetworkDTO)
            .toList();
    }
    
    /**
     * Получить доступные нейросети для клиента
     */
    public List<AvailableNetworkDTO> getAvailableNetworksForClient(ClientApplication clientApp) {
        // Получаем все активные нейросети
        List<NeuralNetwork> allNetworks = neuralNetworkRepository.findByIsActiveTrue();
        
        return allNetworks.stream()
            .filter(network -> isNetworkAccessibleToClient(clientApp, network))
            .map(this::convertToAvailableNetworkDTO)
            .toList();
    }
    
    /**
     * Проверить доступность нейросети (упрощенная версия)
     */
    public boolean isNetworkAvailable(String networkId) {
        Optional<NeuralNetwork> networkOpt = neuralNetworkRepository.findByName(networkId);
        return networkOpt.isPresent() && networkOpt.get().getIsActive();
    }
    
    /**
     * Проверить доступность нейросети для клиента
     */
    public boolean isNetworkAvailableForClient(ClientApplication clientApp, String networkId) {
        Optional<NeuralNetwork> networkOpt = neuralNetworkRepository.findByName(networkId);
        if (networkOpt.isEmpty()) {
            return false;
        }
        
        NeuralNetwork network = networkOpt.get();
        return isNetworkAccessibleToClient(clientApp, network);
    }
    
    /**
     * Получить лимиты нейросети (упрощенная версия)
     */
    public Map<String, Object> getNetworkLimits(String networkId) {
        Map<String, Object> limits = new HashMap<>();
        
        Optional<NeuralNetwork> networkOpt = neuralNetworkRepository.findByName(networkId);
        if (networkOpt.isEmpty()) {
            limits.put("error", "Network not found");
            return limits;
        }
        
        NeuralNetwork network = networkOpt.get();
        
        // Получаем информацию о лимитах
        limits.put("networkId", networkId);
        limits.put("networkName", network.getDisplayName());
        limits.put("isFree", network.getIsFree());
        limits.put("priority", network.getPriority());
        
        // TODO: Добавить реальные лимиты из ClientNetworkAccess
        limits.put("remainingRequestsToday", null); // Пока не реализовано
        limits.put("remainingRequestsMonth", null); // Пока не реализовано
        limits.put("hasLimits", false); // Пока не реализовано
        
        return limits;
    }
    
    /**
     * Получить лимиты нейросети для клиента
     */
    public Map<String, Object> getNetworkLimitsForClient(ClientApplication clientApp, String networkId) {
        Map<String, Object> limits = new HashMap<>();
        
        Optional<NeuralNetwork> networkOpt = neuralNetworkRepository.findByName(networkId);
        if (networkOpt.isEmpty()) {
            limits.put("error", "Network not found");
            return limits;
        }
        
        NeuralNetwork network = networkOpt.get();
        
        // Получаем информацию о лимитах
        limits.put("networkId", networkId);
        limits.put("networkName", network.getDisplayName());
        limits.put("isFree", network.getIsFree());
        limits.put("priority", network.getPriority());
        
        // TODO: Добавить реальные лимиты из ClientNetworkAccess
        limits.put("remainingRequestsToday", null); // Пока не реализовано
        limits.put("remainingRequestsMonth", null); // Пока не реализовано
        limits.put("hasLimits", false); // Пока не реализовано
        
        return limits;
    }
    
    /**
     * Проверить, доступна ли нейросеть клиенту
     */
    private boolean isNetworkAccessibleToClient(ClientApplication clientApp, NeuralNetwork network) {
        // TODO: Реализовать проверку доступа через ClientNetworkAccess
        // Пока что возвращаем true для всех активных сетей
        return network.getIsActive();
    }
    
    /**
     * Конвертировать NeuralNetwork в AvailableNetworkDTO
     */
    private AvailableNetworkDTO convertToAvailableNetworkDTO(NeuralNetwork network) {
        AvailableNetworkDTO dto = new AvailableNetworkDTO();
        dto.setId(network.getId().toString());
        dto.setName(network.getName());
        dto.setDisplayName(network.getDisplayName());
        dto.setProvider(network.getProvider());
        dto.setNetworkType(network.getNetworkType());
        dto.setModelName(network.getModelName());
        dto.setIsFree(network.getIsFree());
        dto.setPriority(network.getPriority());
        dto.setRemainingRequestsToday(null); // Пока не реализовано
        dto.setRemainingRequestsMonth(null); // Пока не реализовано
        dto.setHasLimits(false); // Пока не реализовано
        return dto;
    }
}

