package com.example.integration.service;

import com.example.integration.dto.ClientAppCreateRequest;
import com.example.integration.dto.ClientAppDTO;
import com.example.integration.model.ClientApplication;
import com.example.integration.repository.ClientApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис управления клиентскими приложениями
 */
@Service
public class ClientManagementService {
    
    private final ClientApplicationRepository clientAppRepository;
    
    public ClientManagementService(ClientApplicationRepository clientAppRepository) {
        this.clientAppRepository = clientAppRepository;
    }
    
    /**
     * Получить все клиентские приложения
     */
    @Transactional(readOnly = true)
    public List<ClientAppDTO> getAllClients() {
        return clientAppRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Создать новое клиентское приложение
     */
    @Transactional
    public ClientAppDTO createClient(ClientAppCreateRequest request) {
        ClientApplication client = new ClientApplication();
        client.setName(request.getName());
        client.setDescription(request.getDescription());
        client.setApiKey(generateApiKey());
        client.setIsActive(true);
        
        client = clientAppRepository.save(client);
        return toDTO(client);
    }
    
    /**
     * Обновить клиентское приложение
     */
    @Transactional
    public ClientAppDTO updateClient(UUID id, ClientAppCreateRequest request) {
        ClientApplication client = clientAppRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        
        client.setName(request.getName());
        client.setDescription(request.getDescription());
        
        client = clientAppRepository.save(client);
        return toDTO(client);
    }
    
    /**
     * Деактивировать клиентское приложение
     */
    @Transactional
    public void deactivateClient(UUID id) {
        ClientApplication client = clientAppRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        
        client.setIsActive(false);
        clientAppRepository.save(client);
    }
    
    /**
     * Регенерировать API ключ
     */
    @Transactional
    public ClientAppDTO regenerateApiKey(UUID id) {
        ClientApplication client = clientAppRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        
        client.setApiKey(generateApiKey());
        client = clientAppRepository.save(client);
        return toDTO(client);
    }
    
    private String generateApiKey() {
        return "aikey_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private ClientAppDTO toDTO(ClientApplication client) {
        ClientAppDTO dto = new ClientAppDTO();
        dto.setId(client.getId().toString());
        dto.setName(client.getName());
        dto.setDescription(client.getDescription());
        dto.setApiKey(client.getApiKey());
        dto.setIsActive(client.getIsActive());
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());
        return dto;
    }
}

