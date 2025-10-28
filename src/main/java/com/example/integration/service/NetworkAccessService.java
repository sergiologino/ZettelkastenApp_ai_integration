package com.example.integration.service;

import com.example.integration.dto.ClientNetworkAccessDTO;
import com.example.integration.dto.GrantAccessRequest;
import com.example.integration.model.ClientApplication;
import com.example.integration.model.ClientNetworkAccess;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.repository.ClientApplicationRepository;
import com.example.integration.repository.ClientNetworkAccessRepository;
import com.example.integration.repository.NeuralNetworkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @Autowired
    public NetworkAccessService(ClientNetworkAccessRepository clientNetworkAccessRepository,
                               ClientApplicationRepository clientApplicationRepository,
                               NeuralNetworkRepository neuralNetworkRepository) {
        this.clientNetworkAccessRepository = clientNetworkAccessRepository;
        this.clientApplicationRepository = clientApplicationRepository;
        this.neuralNetworkRepository = neuralNetworkRepository;
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
        log.debug("Получаем доступы для клиента: {}", clientId);
        
        ClientApplication client = clientApplicationRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Клиент не найден: " + clientId));

        return clientNetworkAccessRepository.findByClientApplicationOrderByNeuralNetworkDisplayNameAsc(client)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
        log.debug("Получаем доступные нейросети для клиента: {}", clientId);
        
        return getClientAccesses(clientId);
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
