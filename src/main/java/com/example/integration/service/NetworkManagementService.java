package com.example.integration.service;

import com.example.integration.dto.NetworkCreateRequest;
import com.example.integration.dto.NetworkDTO;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.repository.NeuralNetworkRepository;
import com.example.integration.security.EncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис управления нейросетями
 */
@Service
public class NetworkManagementService {
    
    private final NeuralNetworkRepository neuralNetworkRepository;
    private final EncryptionService encryptionService;
    
    public NetworkManagementService(
        NeuralNetworkRepository neuralNetworkRepository,
        EncryptionService encryptionService
    ) {
        this.neuralNetworkRepository = neuralNetworkRepository;
        this.encryptionService = encryptionService;
    }
    
    /**
     * Получить все нейросети
     */
    @Transactional(readOnly = true)
    public List<NetworkDTO> getAllNetworks() {
        return neuralNetworkRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Получить нейросеть по ID
     */
    @Transactional(readOnly = true)
    public NetworkDTO getNetwork(UUID id) {
        NeuralNetwork network = neuralNetworkRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Network not found"));
        return toDTO(network);
    }
    
    /**
     * Создать новую нейросеть
     */
    @Transactional
    public NetworkDTO createNetwork(NetworkCreateRequest request) {
        NeuralNetwork network = new NeuralNetwork();
        network.setName(request.getName());
        network.setDisplayName(request.getDisplayName());
        network.setProvider(request.getProvider());
        network.setNetworkType(request.getNetworkType());
        network.setApiUrl(request.getApiUrl());
        
        // ✅ Шифруем API ключ перед сохранением в БД
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            String encryptedKey = encryptionService.encrypt(request.getApiKey());
            network.setApiKeyEncrypted(encryptedKey);
        }
        
        network.setModelName(request.getModelName());
        network.setIsActive(request.getIsActive());
        network.setIsFree(request.getIsFree());
        network.setPriority(request.getPriority());
        network.setTimeoutSeconds(request.getTimeoutSeconds());
        network.setMaxRetries(request.getMaxRetries());
        network.setRequestMapping(request.getRequestMapping());
        network.setResponseMapping(request.getResponseMapping());
        network.setConnectionInstruction(request.getConnectionInstruction());
        
        network = neuralNetworkRepository.save(network);
        return toDTO(network);
    }
    
    /**
     * Обновить нейросеть
     */
    @Transactional
    public NetworkDTO updateNetwork(UUID id, NetworkCreateRequest request) {
        NeuralNetwork network = neuralNetworkRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Network not found"));
        
        network.setName(request.getName());
        network.setDisplayName(request.getDisplayName());
        network.setProvider(request.getProvider());
        network.setNetworkType(request.getNetworkType());
        network.setApiUrl(request.getApiUrl());
        
        // ✅ Шифруем новый API ключ, если он предоставлен
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            String encryptedKey = encryptionService.encrypt(request.getApiKey());
            network.setApiKeyEncrypted(encryptedKey);
        }
        
        network.setModelName(request.getModelName());
        network.setIsActive(request.getIsActive());
        network.setIsFree(request.getIsFree());
        network.setPriority(request.getPriority());
        network.setTimeoutSeconds(request.getTimeoutSeconds());
        network.setMaxRetries(request.getMaxRetries());
        network.setRequestMapping(request.getRequestMapping());
        network.setResponseMapping(request.getResponseMapping());
        network.setConnectionInstruction(request.getConnectionInstruction());
        
        network = neuralNetworkRepository.save(network);
        return toDTO(network);
    }
    
    /**
     * Удалить нейросеть
     */
    @Transactional
    public void deleteNetwork(UUID id) {
        neuralNetworkRepository.deleteById(id);
    }
    
    private NetworkDTO toDTO(NeuralNetwork network) {
        NetworkDTO dto = new NetworkDTO();
        dto.setId(network.getId().toString());
        dto.setName(network.getName());
        dto.setDisplayName(network.getDisplayName());
        dto.setProvider(network.getProvider());
        dto.setNetworkType(network.getNetworkType());
        dto.setApiUrl(network.getApiUrl());
        dto.setModelName(network.getModelName());
        dto.setIsActive(network.getIsActive());
        dto.setIsFree(network.getIsFree());
        dto.setPriority(network.getPriority());
        dto.setTimeoutSeconds(network.getTimeoutSeconds());
        dto.setMaxRetries(network.getMaxRetries());
        dto.setRequestMapping(network.getRequestMapping());
        dto.setResponseMapping(network.getResponseMapping());
        dto.setConnectionInstruction(network.getConnectionInstruction());
        dto.setCreatedAt(network.getCreatedAt());
        dto.setUpdatedAt(network.getUpdatedAt());
        return dto;
    }
}

