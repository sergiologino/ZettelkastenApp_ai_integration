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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    private final NetworkAccessService networkAccessService;
    private final SubscriptionLimitService subscriptionLimitService;
    private final UserApiKeyService userApiKeyService;
    private final com.example.integration.repository.UserClientLinkRepository userClientLinkRepository;
    private final com.example.integration.repository.ClientNetworkAccessRepository clientNetworkAccessRepository;
    
    @Value("${ai.enable-fallback:true}")
    private boolean enableFallback;
    
    public AiOrchestrationService(
        NeuralClientFactory clientFactory,
        RateLimitService rateLimitService,
        NeuralNetworkRepository neuralNetworkRepository,
        ExternalUserRepository externalUserRepository,
        RequestLogRepository requestLogRepository,
        ClientApplicationRepository clientAppRepository,
        NetworkAccessService networkAccessService,
        SubscriptionLimitService subscriptionLimitService,
        UserApiKeyService userApiKeyService,
        com.example.integration.repository.UserClientLinkRepository userClientLinkRepository,
        com.example.integration.repository.ClientNetworkAccessRepository clientNetworkAccessRepository
    ) {
        this.clientFactory = clientFactory;
        this.rateLimitService = rateLimitService;
        this.neuralNetworkRepository = neuralNetworkRepository;
        this.externalUserRepository = externalUserRepository;
        this.requestLogRepository = requestLogRepository;
        this.networkAccessService = networkAccessService;
        this.subscriptionLimitService = subscriptionLimitService;
        this.userApiKeyService = userApiKeyService;
        this.userClientLinkRepository = userClientLinkRepository;
        this.clientNetworkAccessRepository = clientNetworkAccessRepository;
    }
    
    /**
     * Обработать AI-запрос
     */
    @Transactional
    public AiResponseDTO processRequest(ClientApplication clientApp, AiRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        log.info("🚀 [AiOrchestrationService] ===== Новый AI запрос =====");
        log.info("   Клиент: {} (ID: {})", clientApp.getName(), clientApp.getId());
        log.info("   UserId: {}", request.getUserId());
        log.info("   Запрошенная нейросеть: {}", request.getNetworkName() != null ? request.getNetworkName() : "автовыбор");
        log.info("   Тип запроса: {}", request.getRequestType());
        
        // 1. Получить или создать пользователя
        ExternalUser user = getOrCreateUser(clientApp, request.getUserId());
        
        // 2. Выбрать нейросеть (с учетом доступов клиента и приоритетов из админки)
        NeuralNetwork network = selectNetwork(clientApp, request.getNetworkName(), request.getRequestType(), user);
        log.info("   ✅ Выбрана нейросеть: {} (ID: {}, name: {})", network.getDisplayName(), network.getId(), network.getName());
        
        // 2.5. Проверить лимиты подписки
        String limitError = subscriptionLimitService.checkRequestLimit(clientApp, network);
        if (limitError != null) {
            // Создаем лог с ошибкой лимита
            RequestLog requestLog = createRequestLog(clientApp, user, network, request);
            requestLog.markFailed(limitError, 0);
            requestLogRepository.save(requestLog);
            
            // Возвращаем ошибку
            AiResponseDTO errorResponse = new AiResponseDTO();
            errorResponse.setRequestId(requestLog.getId().toString());
            errorResponse.setStatus("failed");
            errorResponse.setErrorMessage(limitError);
            errorResponse.setNetworkUsed(network.getName());
            return errorResponse;
        }
        
        // 3. Создать лог запроса
        RequestLog requestLog = createRequestLog(clientApp, user, network, request);
        
        try {
            // 3.5. Получить пользовательский API ключ (если есть)
            Optional<String> userApiKey = Optional.empty();
            try {
                // Пытаемся найти владельца клиента через UserClientLink
                Optional<com.example.integration.model.UserClientLink> linkOpt = 
                    userClientLinkRepository.findByClientApplication(clientApp.getId());
                if (linkOpt.isPresent()) {
                    com.example.integration.model.UserAccount owner = linkOpt.get().getUser();
                    userApiKey = userApiKeyService.getApiKey(owner, clientApp.getId(), network.getId());
                }
            } catch (Exception e) {
                // Если не удалось получить пользовательский ключ, используем системный
                log.debug("Не удалось получить пользовательский API ключ: {}", e.getMessage());
            }
            
            // 4. Отправить запрос в нейросеть
            BaseNeuralClient client = clientFactory.getClient(network);
            // Устанавливаем пользовательский ключ в ThreadLocal, если он есть
            try {
                if (userApiKey.isPresent()) {
                    BaseNeuralClient.setUserApiKey(userApiKey.get());
                }
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
            } finally {
                // Очищаем пользовательский ключ из ThreadLocal
                BaseNeuralClient.clearUserApiKey();
            }
            
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
    
    private NeuralNetwork selectNetwork(ClientApplication clientApp, String networkName, String requestType, ExternalUser user) {
        NeuralNetwork network;
        
        if (networkName != null && !networkName.isEmpty()) {
            // Пользователь указал конкретную нейросеть
            log.info("   🔍 Ищем нейросеть по имени: '{}'", networkName);
            Optional<NeuralNetwork> networkOpt = neuralNetworkRepository.findByName(networkName);
            if (networkOpt.isEmpty()) {
                log.error("   ❌ Нейросеть с именем '{}' не найдена в БД", networkName);
                // Покажем все доступные нейросети для диагностики
                List<NeuralNetwork> allNetworks = neuralNetworkRepository.findAll();
                log.info("   📋 Всего нейросетей в БД: {}", allNetworks.size());
                allNetworks.forEach(n -> {
                    log.info("      - name: '{}', displayName: '{}', id: {}, active: {}", 
                        n.getName(), n.getDisplayName(), n.getId(), n.getIsActive());
                });
                throw new IllegalArgumentException("Network not found: " + networkName);
            }
            network = networkOpt.get();
            log.info("   ✅ Найдена нейросеть: {} (name: '{}', id: {})", network.getDisplayName(), network.getName(), network.getId());
        } else {
            // Автоматический выбор из доступных нейросетей клиента с учетом приоритетов из админки
            log.info("   🔍 Автовыбор нейросети для типа: {} из доступных для клиента {}", requestType, clientApp.getName());
            
            // Получаем все доступы клиента, отсортированные по приоритету (меньше = выше приоритет)
            // Используем прямой запрос к репозиторию для получения доступа с приоритетами
            List<com.example.integration.model.ClientNetworkAccess> clientAccesses = 
                clientNetworkAccessRepository.findByClientApplicationOrderByPriorityAsc(clientApp)
                    .stream()
                    .filter(access -> access.getNeuralNetwork().getIsActive())
                    .filter(access -> {
                        // Фильтруем по типу запроса
                        String networkType = access.getNeuralNetwork().getNetworkType();
                        return networkType != null && networkType.equalsIgnoreCase(requestType);
                    })
                    .filter(access -> rateLimitService.isNetworkAvailable(user, access.getNeuralNetwork()))
                    .collect(java.util.stream.Collectors.toList());
            
            if (clientAccesses.isEmpty()) {
                log.error("   ❌ Нет доступных нейросетей для клиента {} типа {}", clientApp.getName(), requestType);
                throw new IllegalStateException("No available network for client " + clientApp.getName() + " and type: " + requestType);
            }
            
            network = clientAccesses.get(0).getNeuralNetwork();
            Integer priority = clientAccesses.get(0).getPriority();
            log.info("   ✅ Автоматически выбрана нейросеть: {} (name: '{}', id: {}, priority: {})", 
                network.getDisplayName(), network.getName(), network.getId(), priority);
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
        if (response.containsKey("usage") && response.get("usage") instanceof Map<?, ?> usage) {
            Object totalTokens = usage.get("total_tokens");
            if (totalTokens instanceof Number n) {
                return n.intValue();
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
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiOrchestrationService.class);
        log.info("🔍 [AiOrchestrationService] Получаем доступные нейросети для клиента: {} (ID: {})", 
            clientApp.getName(), clientApp.getId());
        
        // Получаем все доступы клиента через NetworkAccessService (возвращает DTO)
        var accesses = networkAccessService.getAvailableNetworks(clientApp.getId());
        log.info("🔍 [AiOrchestrationService] Найдено {} доступов для клиента", accesses.size());
        
        List<AvailableNetworkDTO> networks = accesses.stream()
                .map(access -> {
                    // Получаем полную информацию о нейросети по ID из DTO
                    UUID networkId = access.getNetworkId();
                    log.info("🔍 [AiOrchestrationService] Ищем нейросеть по ID: {}", networkId);
                    
                    Optional<NeuralNetwork> networkOpt = neuralNetworkRepository.findById(networkId);
                    
                    if (networkOpt.isEmpty()) {
                        log.warn("⚠️ [AiOrchestrationService] Нейросеть с ID {} не найдена в БД", networkId);
                        return null;
                    }
                    
                    NeuralNetwork network = networkOpt.get();
                    
                    if (!network.getIsActive()) {
                        log.warn("⚠️ [AiOrchestrationService] Нейросеть {} неактивна (is_active=false)", network.getDisplayName());
                        return null; // Пропускаем неактивные нейросети
                    }
                    
                    log.info("✅ [AiOrchestrationService] Найдена активная нейросеть: {} (тип: {}, provider: {})", 
                        network.getDisplayName(), network.getNetworkType(), network.getProvider());
                    
                    AvailableNetworkDTO dto = convertToAvailableNetworkDTO(network);
                    
                    // Добавляем информацию о лимитах из доступа
                    dto.setRemainingRequestsToday(access.getDailyRequestLimit());
                    dto.setRemainingRequestsMonth(access.getMonthlyRequestLimit());
                    
                    // Проверяем наличие лимитов (null или > 0)
                    boolean hasDailyLimit = access.getDailyRequestLimit() != null && access.getDailyRequestLimit() > 0;
                    boolean hasMonthlyLimit = access.getMonthlyRequestLimit() != null && access.getMonthlyRequestLimit() > 0;
                    dto.setHasLimits(hasDailyLimit || hasMonthlyLimit);
                    
                    log.info("   📊 Лимиты: daily={}, monthly={}, hasLimits={}", 
                        access.getDailyRequestLimit(), access.getMonthlyRequestLimit(), dto.getHasLimits());
                    
                    return dto;
                })
                .filter(dto -> dto != null) // Убираем null значения
                .toList();
        
        log.info("✅ [AiOrchestrationService] Возвращаем {} доступных нейросетей для клиента {}", 
            networks.size(), clientApp.getName());
        networks.forEach(network -> {
            log.info("  - {} (тип: {}, provider: {}, приоритет: {})", 
                network.getDisplayName(), network.getNetworkType(), network.getProvider(), network.getPriority());
        });
        
        return networks;
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
        log.debug("Проверяем доступность нейросети {} для клиента {}", networkId, clientApp.getName());
        
        Optional<NeuralNetwork> networkOpt = neuralNetworkRepository.findByName(networkId);
        if (networkOpt.isEmpty()) {
            log.warn("Нейросеть не найдена: {}", networkId);
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

    public Map<String, Object> getClientNetworkLimits(ClientApplication clientApp, String networkId) {
        Map<String, Object> limits = new HashMap<>();
        limits.put("networkId", networkId);

        networkAccessService.getClientNetworkAccess(clientApp.getId(), networkId).ifPresent(access -> {
            limits.put("networkName", access.getNetworkName());
            limits.put("remainingRequestsToday", access.getDailyRequestLimit());
            limits.put("remainingRequestsMonth", access.getMonthlyRequestLimit());
            boolean hasDaily = access.getDailyRequestLimit() != null && access.getDailyRequestLimit() > 0;
            boolean hasMonthly = access.getMonthlyRequestLimit() != null && access.getMonthlyRequestLimit() > 0;
            limits.put("hasLimits", hasDaily || hasMonthly);
        });

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
        
        // Проверяем доступ
        if (!isNetworkAccessibleToClient(clientApp, network)) {
            limits.put("error", "Network not accessible to client");
            return limits;
        }
        
        // Получаем информацию о лимитах из доступа
        try {
            var access = networkAccessService.getClientAccesses(clientApp.getId())
                    .stream()
                    .filter(a -> a.getNetworkId().equals(network.getId()))
                    .findFirst();
            
            if (access.isPresent()) {
                var clientAccess = access.get();
                limits.put("networkId", networkId);
                limits.put("networkName", network.getDisplayName());
                limits.put("isFree", network.getIsFree());
                limits.put("priority", network.getPriority());
                limits.put("remainingRequestsToday", clientAccess.getDailyRequestLimit());
                limits.put("remainingRequestsMonth", clientAccess.getMonthlyRequestLimit());
                limits.put("hasLimits", clientAccess.hasDailyLimit() || clientAccess.hasMonthlyLimit());
            } else {
                limits.put("error", "Access not found");
            }
        } catch (Exception e) {
            log.error("Ошибка получения лимитов для клиента", e);
            limits.put("error", "Failed to get limits");
        }
        
        return limits;
    }
    
    /**
     * Проверить, доступна ли нейросеть клиенту
     */
    private boolean isNetworkAccessibleToClient(ClientApplication clientApp, NeuralNetwork network) {
        // Проверяем доступ через NetworkAccessService
        return networkAccessService.isNetworkAvailable(clientApp.getId(), network.getId());
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

