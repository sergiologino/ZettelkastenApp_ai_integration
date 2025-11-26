package com.example.integration.service;

import com.example.integration.dto.ClientNetworkAccessDTO;
import com.example.integration.dto.GrantAccessRequest;
import com.example.integration.dto.UserAccessGroupDto;
import com.example.integration.dto.ClientServiceDto;
import com.example.integration.dto.NetworkAccessInfoDto;
import com.example.integration.model.ClientApplication;
import com.example.integration.model.ClientNetworkAccess;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.model.UserAccount;
import com.example.integration.repository.ClientApplicationRepository;
import com.example.integration.repository.ClientNetworkAccessRepository;
import com.example.integration.repository.NeuralNetworkRepository;
import com.example.integration.repository.UserAccountRepository;
import com.example.integration.repository.UserClientLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для управления доступом клиентов к нейросетям
 */
@Service
@Transactional
public class NetworkAccessService {

    private static final Logger log = LoggerFactory.getLogger(NetworkAccessService.class);

    private final ClientNetworkAccessRepository clientNetworkAccessRepository;
    private final ClientApplicationRepository clientApplicationRepository;
    private final NeuralNetworkRepository neuralNetworkRepository;
    private final UserClientLinkRepository userClientLinkRepository;
    private final UserAccountRepository userAccountRepository;

    @Autowired
    public NetworkAccessService(ClientNetworkAccessRepository clientNetworkAccessRepository,
                               ClientApplicationRepository clientApplicationRepository,
                               NeuralNetworkRepository neuralNetworkRepository,
                               UserClientLinkRepository userClientLinkRepository,
                               UserAccountRepository userAccountRepository) {
        this.clientNetworkAccessRepository = clientNetworkAccessRepository;
        this.clientApplicationRepository = clientApplicationRepository;
        this.neuralNetworkRepository = neuralNetworkRepository;
        this.userClientLinkRepository = userClientLinkRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Предоставить доступ клиенту к нейросети
     */
    public ClientNetworkAccessDTO grantAccess(GrantAccessRequest request) {
        log.info("Предоставляем доступ клиенту {} к нейросети {}", request.getClientId(), request.getNetworkId());

        // Проверяем существование клиента и нейросети
        ClientApplication client = clientApplicationRepository.findById(request.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден: " + request.getClientId()));
        
        NeuralNetwork network = neuralNetworkRepository.findById(request.getNetworkId())
                .orElseThrow(() -> new IllegalArgumentException("Нейросеть не найдена: " + request.getNetworkId()));

        // Проверяем, не существует ли уже доступ
        Optional<ClientNetworkAccess> existingAccess = clientNetworkAccessRepository
                .findByClientApplicationAndNeuralNetwork(client, network);

        ClientNetworkAccess access;
        if (existingAccess.isPresent()) {
            // Обновляем существующий доступ
            access = existingAccess.get();
            access.setDailyRequestLimit(request.getDailyRequestLimit());
            access.setMonthlyRequestLimit(request.getMonthlyRequestLimit());
            log.info("Обновлен существующий доступ для клиента {} к нейросети {}", client.getName(), network.getName());
        } else {
            // Создаем новый доступ
            access = new ClientNetworkAccess(client, network, 
                    request.getDailyRequestLimit(), request.getMonthlyRequestLimit());
            log.info("Создан новый доступ для клиента {} к нейросети {}", client.getName(), network.getName());
        }

        ClientNetworkAccess savedAccess = clientNetworkAccessRepository.save(access);
        return convertToDTO(savedAccess);
    }

    /**
     * Отозвать доступ клиента к нейросети
     */
    public void revokeAccess(UUID accessId) {
        log.info("Отзываем доступ с ID: {}", accessId);
        
        ClientNetworkAccess access = clientNetworkAccessRepository.findById(accessId)
                .orElseThrow(() -> new IllegalArgumentException("Доступ не найден: " + accessId));

        clientNetworkAccessRepository.delete(access);
        log.info("Доступ отозван для клиента {} к нейросети {}", 
                access.getClientApplication().getName(), access.getNeuralNetwork().getName());
    }

    /**
     * Отозвать доступ по клиенту и нейросети
     */
    public void revokeAccess(UUID clientId, UUID networkId) {
        log.info("Отзываем доступ клиента {} к нейросети {}", clientId, networkId);
        
        ClientNetworkAccess access = clientNetworkAccessRepository
                .findByClientIdAndNetworkId(clientId, networkId)
                .orElseThrow(() -> new IllegalArgumentException("Доступ не найден для клиента " + clientId + " и нейросети " + networkId));

        clientNetworkAccessRepository.delete(access);
        log.info("Доступ отозван для клиента {} к нейросети {}", 
                access.getClientApplication().getName(), access.getNeuralNetwork().getName());
    }

    /**
     * Получить все доступы
     */
    @Transactional(readOnly = true)
    public List<ClientNetworkAccessDTO> getAllAccesses() {
        log.debug("Получаем все доступы клиентов к нейросетям");
        
        return clientNetworkAccessRepository.findAllOrderedByClientAndNetwork()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получить доступ по ID
     */
    @Transactional(readOnly = true)
    public ClientNetworkAccessDTO getAccessById(UUID accessId) {
        log.debug("Получаем доступ по ID: {}", accessId);
        
        ClientNetworkAccess access = clientNetworkAccessRepository.findById(accessId)
                .orElseThrow(() -> new IllegalArgumentException("Доступ не найден: " + accessId));

        return convertToDTO(access);
    }

    /**
     * Получить доступы для конкретного клиента
     */
    @Transactional(readOnly = true)
    public List<ClientNetworkAccessDTO> getClientAccesses(UUID clientId) {
        log.info("🔍 [NetworkAccessService] Получаем все доступы для клиента: {}", clientId);
        
        ClientApplication client = clientApplicationRepository.findById(clientId)
                .orElseThrow(() -> {
                    log.error("❌ [NetworkAccessService] Клиент не найден: {}", clientId);
                    return new IllegalArgumentException("Клиент не найден: " + clientId);
                });
        
        log.info("✅ [NetworkAccessService] Клиент найден: {} (ID: {})", client.getName(), client.getId());

        List<ClientNetworkAccessDTO> accesses = clientNetworkAccessRepository.findByClientApplicationOrderByNeuralNetworkDisplayNameAsc(client)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        log.info("📋 [NetworkAccessService] Найдено {} доступов в БД для клиента {}", accesses.size(), client.getName());
        accesses.forEach(access -> {
            log.debug("  - Доступ к нейросети ID: {}, DisplayName: {}", 
                access.getNetworkId(), access.getNetworkDisplayName());
        });
        
        return accesses;
    }

    /**
     * Получить доступы для конкретной нейросети
     */
    @Transactional(readOnly = true)
    public List<ClientNetworkAccessDTO> getNetworkAccesses(UUID networkId) {
        log.debug("Получаем доступы для нейросети: {}", networkId);
        
        NeuralNetwork network = neuralNetworkRepository.findById(networkId)
                .orElseThrow(() -> new IllegalArgumentException("Нейросеть не найдена: " + networkId));

        return clientNetworkAccessRepository.findByNeuralNetworkOrderByClientApplicationNameAsc(network)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Проверить, есть ли доступ у клиента к нейросети
     */
    @Transactional(readOnly = true)
    public boolean isNetworkAvailable(UUID clientId, UUID networkId) {
        log.debug("Проверяем доступ клиента {} к нейросети {}", clientId, networkId);
        
        return clientNetworkAccessRepository.existsByClientIdAndNetworkId(clientId, networkId);
    }

    /**
     * Получить доступные нейросети для клиента
     */
    @Transactional(readOnly = true)
    public List<ClientNetworkAccessDTO> getAvailableNetworks(UUID clientId) {
        log.info("🔍 [NetworkAccessService] Получаем доступные нейросети для клиента: {}", clientId);
        
        List<ClientNetworkAccessDTO> accesses = getClientAccesses(clientId);
        log.info("✅ [NetworkAccessService] Найдено {} доступов для клиента {}", accesses.size(), clientId);
        accesses.forEach(access -> {
            log.debug("  - Нейросеть ID: {}, DisplayName: {}", access.getNetworkId(), access.getNetworkDisplayName());
        });
        
        return accesses;
    }

    @Transactional(readOnly = true)
    public Optional<ClientNetworkAccessDTO> getClientNetworkAccess(UUID clientId, String networkName) {
        return getClientAccesses(clientId).stream()
                .filter(dto -> dto.getNetworkName().equalsIgnoreCase(networkName))
                .findFirst();
    }

    /**
     * Получить статистику доступов
     */
    @Transactional(readOnly = true)
    public AccessStats getAccessStats() {
        log.debug("Получаем статистику доступов");
        
        long totalAccesses = clientNetworkAccessRepository.count();
        long accessesWithLimits = clientNetworkAccessRepository.findWithLimits().size();
        long unlimitedAccesses = clientNetworkAccessRepository.findWithoutLimits().size();

        return new AccessStats(totalAccesses, accessesWithLimits, unlimitedAccesses);
    }
    
    /**
     * Получить доступы, сгруппированные по пользователям: Пользователь → Сервисы → Нейросети
     */
    @Transactional(readOnly = true)
    public List<UserAccessGroupDto> getGroupedAccesses() {
        log.info("Получаем доступы, сгруппированные по пользователям");
        
        // Получаем все доступы
        List<ClientNetworkAccess> allAccesses = clientNetworkAccessRepository.findAll();
        
        // Группируем по клиентам
        Map<UUID, List<ClientNetworkAccess>> accessesByClient = allAccesses.stream()
                .collect(Collectors.groupingBy(access -> access.getClientApplication().getId()));
        
        // Создаем карту: clientId -> userId (если есть)
        Map<UUID, UUID> clientToUserMap = new HashMap<>();
        Map<UUID, UserAccount> userMap = new HashMap<>();
        
        // Заполняем карту связей клиент-пользователь
        for (UUID clientId : accessesByClient.keySet()) {
            userClientLinkRepository.findByClientApplication(clientId)
                    .ifPresent(link -> {
                        clientToUserMap.put(clientId, link.getUser().getId());
                        userMap.put(link.getUser().getId(), link.getUser());
                    });
        }
        
        // Группируем по пользователям
        Map<UUID, Map<UUID, List<ClientNetworkAccess>>> userClientAccessMap = new HashMap<>();
        
        // Добавляем админские сервисы (без пользователя)
        UUID adminUserId = null; // null означает админские сервисы
        
        for (Map.Entry<UUID, List<ClientNetworkAccess>> entry : accessesByClient.entrySet()) {
            UUID clientId = entry.getKey();
            UUID userId = clientToUserMap.get(clientId);
            
            if (userId == null) {
                // Админский сервис
                userClientAccessMap.computeIfAbsent(adminUserId, k -> new HashMap<>())
                        .put(clientId, entry.getValue());
            } else {
                // Сервис пользователя
                userClientAccessMap.computeIfAbsent(userId, k -> new HashMap<>())
                        .put(clientId, entry.getValue());
            }
        }
        
        // Создаем результат
        List<UserAccessGroupDto> result = new ArrayList<>();
        
        // Обрабатываем пользователей
        for (Map.Entry<UUID, Map<UUID, List<ClientNetworkAccess>>> userEntry : userClientAccessMap.entrySet()) {
            UUID userId = userEntry.getKey();
            
            if (userId == null) {
                // Админские сервисы - создаем специальную группу
                UserAccessGroupDto adminGroup = new UserAccessGroupDto();
                adminGroup.setUserId(null);
                adminGroup.setUserEmail("Администратор");
                adminGroup.setUserFullName("Администратор");
                
                List<ClientServiceDto> adminServices = new ArrayList<>();
                for (Map.Entry<UUID, List<ClientNetworkAccess>> clientEntry : userEntry.getValue().entrySet()) {
                    UUID clientId = clientEntry.getKey();
                    ClientApplication client = clientApplicationRepository.findById(clientId).orElse(null);
                    if (client == null) continue;
                    
                    ClientServiceDto serviceDto = new ClientServiceDto();
                    serviceDto.setClientId(clientId);
                    serviceDto.setClientName(client.getName());
                    serviceDto.setClientDescription(client.getDescription());
                    serviceDto.setIsAdminService(true);
                    
                    List<NetworkAccessInfoDto> networks = clientEntry.getValue().stream()
                            .map(access -> {
                                NetworkAccessInfoDto networkDto = new NetworkAccessInfoDto();
                                networkDto.setAccessId(access.getId());
                                networkDto.setNetworkId(access.getNeuralNetwork().getId());
                                networkDto.setNetworkDisplayName(access.getNeuralNetwork().getDisplayName());
                                networkDto.setNetworkProvider(access.getNeuralNetwork().getProvider());
                                networkDto.setNetworkType(access.getNeuralNetwork().getNetworkType());
                                networkDto.setDailyRequestLimit(access.getDailyRequestLimit());
                                networkDto.setMonthlyRequestLimit(access.getMonthlyRequestLimit());
                                networkDto.setPriority(access.getPriority());
                                return networkDto;
                            })
                            .sorted(Comparator.comparing(NetworkAccessInfoDto::getPriority, 
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .collect(Collectors.toList());
                    
                    serviceDto.setNetworks(networks);
                    adminServices.add(serviceDto);
                }
                
                adminGroup.setServices(adminServices);
                result.add(adminGroup);
            } else {
                // Сервисы пользователя
                UserAccount user = userMap.get(userId);
                if (user == null) {
                    user = userAccountRepository.findById(userId).orElse(null);
                    if (user == null) continue;
                }
                
                UserAccessGroupDto userGroup = new UserAccessGroupDto();
                userGroup.setUserId(userId);
                userGroup.setUserEmail(user.getEmail());
                userGroup.setUserFullName(user.getFullName());
                
                List<ClientServiceDto> userServices = new ArrayList<>();
                for (Map.Entry<UUID, List<ClientNetworkAccess>> clientEntry : userEntry.getValue().entrySet()) {
                    UUID clientId = clientEntry.getKey();
                    ClientApplication client = clientApplicationRepository.findById(clientId).orElse(null);
                    if (client == null) continue;
                    
                    ClientServiceDto serviceDto = new ClientServiceDto();
                    serviceDto.setClientId(clientId);
                    serviceDto.setClientName(client.getName());
                    serviceDto.setClientDescription(client.getDescription());
                    serviceDto.setIsAdminService(false);
                    
                    List<NetworkAccessInfoDto> networks = clientEntry.getValue().stream()
                            .map(access -> {
                                NetworkAccessInfoDto networkDto = new NetworkAccessInfoDto();
                                networkDto.setAccessId(access.getId());
                                networkDto.setNetworkId(access.getNeuralNetwork().getId());
                                networkDto.setNetworkDisplayName(access.getNeuralNetwork().getDisplayName());
                                networkDto.setNetworkProvider(access.getNeuralNetwork().getProvider());
                                networkDto.setNetworkType(access.getNeuralNetwork().getNetworkType());
                                networkDto.setDailyRequestLimit(access.getDailyRequestLimit());
                                networkDto.setMonthlyRequestLimit(access.getMonthlyRequestLimit());
                                networkDto.setPriority(access.getPriority());
                                return networkDto;
                            })
                            .sorted(Comparator.comparing(NetworkAccessInfoDto::getPriority, 
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .collect(Collectors.toList());
                    
                    serviceDto.setNetworks(networks);
                    userServices.add(serviceDto);
                }
                
                userGroup.setServices(userServices);
                result.add(userGroup);
            }
        }
        
        // Сортируем: сначала админские, потом пользователи по email
        result.sort((a, b) -> {
            if (a.getUserId() == null && b.getUserId() != null) return -1;
            if (a.getUserId() != null && b.getUserId() == null) return 1;
            if (a.getUserId() == null && b.getUserId() == null) return 0;
            return a.getUserEmail().compareToIgnoreCase(b.getUserEmail());
        });
        
        return result;
    }

    /**
     * Конвертировать ClientNetworkAccess в DTO
     */
    private ClientNetworkAccessDTO convertToDTO(ClientNetworkAccess access) {
        return new ClientNetworkAccessDTO(
                access.getId(),
                access.getClientApplication().getId(),
                access.getClientApplication().getName(),
                access.getNeuralNetwork().getId(),
                access.getNeuralNetwork().getDisplayName(),
                access.getNeuralNetwork().getProvider(),
                access.getNeuralNetwork().getNetworkType(),
                access.getDailyRequestLimit(),
                access.getMonthlyRequestLimit(),
                access.getCreatedAt(),
                access.getUpdatedAt()
        );
    }

    /**
     * Предоставить доступ клиенту КО ВСЕМ активным нейросетям
     */
    public java.util.Map<String, Object> grantAccessToAllNetworks(UUID clientId) {
        log.info("🔗 [NetworkAccessService] ===== Начало предоставления доступа ко всем сетям =====");
        log.info("   ClientId: {}", clientId);
        
        // Проверяем существование клиента
        ClientApplication client = clientApplicationRepository.findById(clientId)
                .orElseThrow(() -> {
                    log.error("❌ Клиент не найден: {}", clientId);
                    return new IllegalArgumentException("Клиент не найден: " + clientId);
                });
        
        log.info("✅ Клиент найден: {} (активен: {})", client.getName(), client.getIsActive());
        
        // Получаем все активные нейросети
        List<NeuralNetwork> activeNetworks = neuralNetworkRepository.findByIsActiveTrue();
        log.info("📡 Найдено активных нейросетей: {}", activeNetworks.size());
        
        if (activeNetworks.isEmpty()) {
            log.warn("⚠️ Нет активных нейросетей для предоставления доступа");
            return java.util.Map.of(
                "message", "Нет активных нейросетей",
                "granted", 0,
                "skipped", 0,
                "total", 0
            );
        }
        
        int granted = 0;
        int skipped = 0;
        java.util.List<String> grantedNetworks = new java.util.ArrayList<>();
        java.util.List<String> skippedNetworks = new java.util.ArrayList<>();
        
        // Предоставляем доступ ко всем активным нейросетям
        for (NeuralNetwork network : activeNetworks) {
            try {
                // Проверяем, не существует ли уже доступ
                Optional<ClientNetworkAccess> existingAccess = clientNetworkAccessRepository
                        .findByClientApplicationAndNeuralNetwork(client, network);
                
                if (existingAccess.isPresent()) {
                    log.debug("⏭️ Доступ к {} уже существует, пропускаем", network.getName());
                    skipped++;
                    skippedNetworks.add(network.getDisplayName() + " (уже есть доступ)");
                    continue;
                }
                
                // Создаем новый доступ без лимитов (unlimited)
                ClientNetworkAccess access = new ClientNetworkAccess(client, network, null, null);
                clientNetworkAccessRepository.save(access);
                
                log.info("✅ Предоставлен доступ к: {} ({})", network.getDisplayName(), network.getNetworkType());
                granted++;
                grantedNetworks.add(network.getDisplayName() + " (" + network.getNetworkType() + ")");
                
            } catch (Exception e) {
                log.error("❌ Ошибка предоставления доступа к {}: {}", network.getName(), e.getMessage());
                skipped++;
                skippedNetworks.add(network.getDisplayName() + " (ошибка)");
            }
        }
        
        log.info("===== Завершено предоставление доступа =====");
        log.info("   Предоставлено: {}", granted);
        log.info("   Пропущено: {}", skipped);
        log.info("   Всего сетей: {}", activeNetworks.size());
        
        return java.util.Map.of(
            "message", String.format("Предоставлен доступ к %d нейросетям (пропущено: %d)", granted, skipped),
            "granted", granted,
            "skipped", skipped,
            "total", activeNetworks.size(),
            "grantedNetworks", grantedNetworks,
            "skippedNetworks", skippedNetworks,
            "clientName", client.getName()
        );
    }
    
    /**
     * Статистика доступов
     */
    public static class AccessStats {
        private final long totalAccesses;
        private final long accessesWithLimits;
        private final long unlimitedAccesses;

        public AccessStats(long totalAccesses, long accessesWithLimits, long unlimitedAccesses) {
            this.totalAccesses = totalAccesses;
            this.accessesWithLimits = accessesWithLimits;
            this.unlimitedAccesses = unlimitedAccesses;
        }

        public long getTotalAccesses() {
            return totalAccesses;
        }

        public long getAccessesWithLimits() {
            return accessesWithLimits;
        }

        public long getUnlimitedAccesses() {
            return unlimitedAccesses;
        }
    }
}
