package com.example.integration.service;

import com.example.integration.dto.user.AvailableNetworkDto;
import com.example.integration.dto.user.ClientApplicationDto;
import com.example.integration.dto.user.ClientCreateRequest;
import com.example.integration.dto.user.ClientUpdateRequest;
import com.example.integration.dto.user.NetworkUsageStatsDto;
import com.example.integration.model.ClientApplication;
import com.example.integration.model.ClientNetworkAccess;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.model.UserAccount;
import com.example.integration.model.UserClientLink;
import com.example.integration.repository.ClientApplicationRepository;
import com.example.integration.repository.ClientNetworkAccessRepository;
import com.example.integration.repository.NeuralNetworkRepository;
import com.example.integration.repository.RequestLogRepository;
import com.example.integration.repository.UserClientLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserClientService {

    private final ClientApplicationRepository clientApplicationRepository;
    private final UserClientLinkRepository linkRepository;
    private final NeuralNetworkRepository neuralNetworkRepository;
    private final RequestLogRepository requestLogRepository;
    private final ClientNetworkAccessRepository clientNetworkAccessRepository;

    public UserClientService(ClientApplicationRepository clientApplicationRepository,
                             UserClientLinkRepository linkRepository,
                             NeuralNetworkRepository neuralNetworkRepository,
                             RequestLogRepository requestLogRepository,
                             ClientNetworkAccessRepository clientNetworkAccessRepository) {
        this.clientApplicationRepository = clientApplicationRepository;
        this.linkRepository = linkRepository;
        this.neuralNetworkRepository = neuralNetworkRepository;
        this.requestLogRepository = requestLogRepository;
        this.clientNetworkAccessRepository = clientNetworkAccessRepository;
    }

    public List<ClientApplicationDto> list(UserAccount user) {
        return linkRepository.findActiveByUserId(user.getId())
                .stream()
                .map(link -> toDto(link.getClientApplication()))
                .collect(Collectors.toList());
    }

    public ClientApplicationDto create(UserAccount user, ClientCreateRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new IllegalArgumentException("Название обязательно");
        }
        ClientApplication app = new ClientApplication();
        app.setName(req.getName().trim());
        app.setDescription(StringUtils.hasText(req.getDescription()) ? req.getDescription().trim() : null);
        app.setApiKey(generateApiKey());
        app.setIsActive(true);
        app.setDeleted(false);
        clientApplicationRepository.save(app);

        UserClientLink link = new UserClientLink();
        link.setUser(user);
        link.setClientApplication(app);
        linkRepository.save(link);

        return toDto(app);
    }

    public Optional<ClientApplication> ensureOwned(UserAccount user, UUID clientId) {
        return linkRepository.findByUserAndClient(user.getId(), clientId)
                .map(UserClientLink::getClientApplication);
    }

    public ClientApplicationDto update(UserAccount user, UUID id, ClientUpdateRequest req) {
        ClientApplication app = ensureOwned(user, id)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден или недоступен"));
        if (StringUtils.hasText(req.getName())) {
            app.setName(req.getName().trim());
        }
        if (req.getDescription() != null) {
            app.setDescription(StringUtils.hasText(req.getDescription()) ? req.getDescription().trim() : null);
        }
        if (req.getIsActive() != null) {
            app.setIsActive(req.getIsActive());
        }
        clientApplicationRepository.save(app);
        return toDto(app);
    }

    public void delete(UserAccount user, UUID id) {
        ClientApplication app = ensureOwned(user, id)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден или недоступен"));
        app.setDeleted(true);
        clientApplicationRepository.save(app);
    }

    public ClientApplicationDto regenerateKey(UserAccount user, UUID id) {
        ClientApplication app = ensureOwned(user, id)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден или недоступен"));
        app.setApiKey(generateApiKey());
        clientApplicationRepository.save(app);
        return toDto(app);
    }

    private ClientApplicationDto toDto(ClientApplication app) {
        ClientApplicationDto dto = new ClientApplicationDto();
        dto.setId(app.getId());
        dto.setName(app.getName());
        dto.setDescription(app.getDescription());
        dto.setApiKey(app.getApiKey());
        dto.setIsActive(app.getIsActive());
        dto.setDeleted(app.getDeleted());
        
        // Загружаем подключенные сети
        List<ClientNetworkAccess> accesses = clientNetworkAccessRepository
                .findByClientApplicationOrderByNeuralNetworkDisplayNameAsc(app);
        List<UUID> networkIds = accesses.stream()
                .map(access -> access.getNeuralNetwork().getId())
                .collect(Collectors.toList());
        dto.setNetworkIds(networkIds);
        
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        return dto;
    }

    /**
     * Получить список доступных активных нейросетей
     */
    public List<AvailableNetworkDto> getAvailableNetworks() {
        return neuralNetworkRepository.findByIsActiveTrue()
                .stream()
                .map(this::toAvailableNetworkDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Получить список подключенных нейросетей для клиента с приоритетами
     */
    public List<com.example.integration.dto.user.ClientNetworkAccessDto> getClientNetworksWithPriority(UserAccount user, UUID clientId) {
        ClientApplication client = ensureOwned(user, clientId)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден или недоступен"));
        
        List<ClientNetworkAccess> accesses = clientNetworkAccessRepository
                .findByClientApplicationOrderByPriorityAsc(client);
        
        return accesses.stream()
                .map(access -> {
                    NeuralNetwork network = access.getNeuralNetwork();
                    com.example.integration.dto.user.ClientNetworkAccessDto dto = new com.example.integration.dto.user.ClientNetworkAccessDto();
                    dto.setNetworkId(network.getId());
                    dto.setNetworkName(network.getName());
                    dto.setNetworkDisplayName(network.getDisplayName());
                    dto.setProvider(network.getProvider());
                    dto.setNetworkType(network.getNetworkType());
                    dto.setPriority(access.getPriority() != null ? access.getPriority() : 100);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Сохранить список подключенных нейросетей для клиента
     */
    public void setClientNetworks(UserAccount user, UUID clientId, List<UUID> networkIds) {
        // Для обратной совместимости - преобразуем в формат с приоритетами
        List<com.example.integration.dto.user.NetworkWithPriority> networks = networkIds.stream()
                .map(networkId -> {
                    com.example.integration.dto.user.NetworkWithPriority nwp = new com.example.integration.dto.user.NetworkWithPriority();
                    nwp.setNetworkId(networkId);
                    nwp.setPriority(100); // По умолчанию
                    return nwp;
                })
                .collect(java.util.stream.Collectors.toList());
        setClientNetworksWithPriority(user, clientId, networks);
    }
    
    /**
     * Сохранить список подключенных нейросетей для клиента с приоритетами
     */
    public void setClientNetworksWithPriority(UserAccount user, UUID clientId, 
                                             List<com.example.integration.dto.user.NetworkWithPriority> networks) {
        // Проверяем, что клиент принадлежит пользователю
        ClientApplication client = ensureOwned(user, clientId)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден или недоступен"));
        
        // Получаем текущие подключения
        List<ClientNetworkAccess> currentAccesses = clientNetworkAccessRepository
                .findByClientApplicationOrderByNeuralNetworkDisplayNameAsc(client);
        
        // Удаляем все текущие подключения
        clientNetworkAccessRepository.deleteAll(currentAccesses);
        
        // Создаем новые подключения для выбранных сетей с приоритетами
        for (com.example.integration.dto.user.NetworkWithPriority nwp : networks) {
            NeuralNetwork network = neuralNetworkRepository.findById(nwp.getNetworkId())
                    .orElseThrow(() -> new IllegalArgumentException("Нейросеть не найдена: " + nwp.getNetworkId()));
            
            // Проверяем, что сеть активна
            if (!network.getIsActive()) {
                throw new IllegalArgumentException("Нейросеть не активна: " + network.getName());
            }
            
            ClientNetworkAccess access = new ClientNetworkAccess(client, network);
            access.setPriority(nwp.getPriority() != null ? nwp.getPriority() : 100);
            clientNetworkAccessRepository.save(access);
        }
    }

    /**
     * Получить статистику использования нейросетей для клиента (только подключенные сети)
     */
    public List<NetworkUsageStatsDto> getNetworkUsageStats(UserAccount user, UUID clientId) {
        // Проверяем, что клиент принадлежит пользователю
        ClientApplication client = ensureOwned(user, clientId)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден или недоступен"));
        
        // Получаем только подключенные нейросети
        List<ClientNetworkAccess> accesses = clientNetworkAccessRepository
                .findByClientApplicationOrderByNeuralNetworkDisplayNameAsc(client);
        
        return accesses.stream()
                .map(access -> {
                    NeuralNetwork network = access.getNeuralNetwork();
                    NetworkUsageStatsDto stats = new NetworkUsageStatsDto();
                    stats.setNetworkId(network.getId());
                    stats.setNetworkName(network.getName());
                    stats.setNetworkDisplayName(network.getDisplayName());
                    stats.setProvider(network.getProvider());
                    stats.setNetworkType(network.getNetworkType());
                    
                    // Получаем статистику использования
                    Long totalRequests = requestLogRepository.countByClientAndNetwork(client.getId(), network.getId());
                    Long successfulRequests = requestLogRepository.countSuccessfulByClientAndNetwork(client.getId(), network.getId());
                    Long failedRequests = requestLogRepository.countFailedByClientAndNetwork(client.getId(), network.getId());
                    Long totalTokensUsed = requestLogRepository.sumTokensByClientAndNetwork(client.getId(), network.getId());
                    
                    stats.setTotalRequests(totalRequests != null ? totalRequests : 0L);
                    stats.setSuccessfulRequests(successfulRequests != null ? successfulRequests : 0L);
                    stats.setFailedRequests(failedRequests != null ? failedRequests : 0L);
                    stats.setTotalTokensUsed(totalTokensUsed != null ? totalTokensUsed : 0L);
                    stats.setAvailableTokens(null); // TODO: добавить логику расчета доступных токенов на основе лимитов
                    
                    return stats;
                })
                .collect(Collectors.toList());
    }

    private AvailableNetworkDto toAvailableNetworkDto(NeuralNetwork network) {
        AvailableNetworkDto dto = new AvailableNetworkDto();
        dto.setId(network.getId());
        dto.setCode(network.getName());
        dto.setLabel(network.getDisplayName());
        dto.setProvider(network.getProvider());
        dto.setNetworkType(network.getNetworkType());
        dto.setConnectionInstruction(network.getConnectionInstruction());
        return dto;
    }

    private String generateApiKey() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }
}


