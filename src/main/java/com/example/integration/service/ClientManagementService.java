package com.example.integration.service;

import com.example.integration.dto.AssignClientUserRequest;
import com.example.integration.dto.ClientAppCreateRequest;
import com.example.integration.dto.ClientAppDTO;
import com.example.integration.model.ClientApplication;
import com.example.integration.model.UserAccount;
import com.example.integration.model.UserClientLink;
import com.example.integration.repository.ClientApplicationRepository;
import com.example.integration.repository.UserAccountRepository;
import com.example.integration.repository.UserClientLinkRepository;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис управления клиентскими приложениями
 */
@Service
public class ClientManagementService {
    
    private static final Logger log = LoggerFactory.getLogger(ClientManagementService.class);
    
    private final ClientApplicationRepository clientAppRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserClientLinkRepository userClientLinkRepository;
    
    public ClientManagementService(
        ClientApplicationRepository clientAppRepository,
        UserAccountRepository userAccountRepository,
        UserClientLinkRepository userClientLinkRepository
    ) {
        this.clientAppRepository = clientAppRepository;
        this.userAccountRepository = userAccountRepository;
        this.userClientLinkRepository = userClientLinkRepository;
    }
    
    /**
     * Получить все неудаленные клиентские приложения
     */
    @Transactional(readOnly = true)
    public List<ClientAppDTO> getAllClients() {
        return clientAppRepository.findByDeletedFalse().stream()
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
    @SuppressWarnings("null")
    public ClientAppDTO updateClient(@NotNull UUID id, ClientAppCreateRequest request) {
        UUID clientId = requireClientId(id);
        ClientApplication client = clientAppRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        
        client.setName(request.getName());
        client.setDescription(request.getDescription());
        
        client = clientAppRepository.save(client);
        return toDTO(client);
    }
    
    /**
     * Мягкое удаление клиентского приложения
     */
    @Transactional
    @SuppressWarnings("null")
    public void deleteClient(@NotNull UUID id) {
        UUID clientId = requireClientId(id);
        log.info("🗑️ [Admin] Попытка удаления клиента с ID: {}", clientId);
        
        ClientApplication client = clientAppRepository.findById(clientId)
            .orElseThrow(() -> {
                log.error("❌ [Admin] Клиент с ID {} не найден", clientId);
                return new IllegalArgumentException("Client not found");
            });
        
        log.info("📋 [Admin] Найден клиент: {} (удален: {})", client.getName(), client.getDeleted());
        
        if (client.getDeleted()) {
            log.warn("⚠️ [Admin] Клиент {} уже удален", client.getName());
            return;
        }
        
        client.setDeleted(true);
        ClientApplication savedClient = clientAppRepository.save(client);
        
        log.info("✅ [Admin] Клиент {} успешно удален (ID: {})", savedClient.getName(), savedClient.getId());
    }
    
    /**
     * Деактивировать клиентское приложение
     */
    @Transactional
    @SuppressWarnings("null")
    public void deactivateClient(@NotNull UUID id) {
        UUID clientId = requireClientId(id);
        log.info("🔍 [Admin] Попытка деактивации клиента с ID: {}", clientId);
        
        ClientApplication client = clientAppRepository.findById(clientId)
            .orElseThrow(() -> {
                log.error("❌ [Admin] Клиент с ID {} не найден", clientId);
                return new IllegalArgumentException("Client not found");
            });
        
        log.info("📋 [Admin] Найден клиент: {} (активен: {})", client.getName(), client.getIsActive());
        
        if (!client.getIsActive()) {
            log.warn("⚠️ [Admin] Клиент {} уже деактивирован", client.getName());
            return;
        }
        
        client.setIsActive(false);
        ClientApplication savedClient = clientAppRepository.save(client);
        
        log.info("✅ [Admin] Клиент {} успешно деактивирован (ID: {})", savedClient.getName(), savedClient.getId());
    }
    
    /**
     * Активировать клиентское приложение
     */
    @Transactional
    @SuppressWarnings("null")
    public void activateClient(@NotNull UUID id) {
        UUID clientId = requireClientId(id);
        log.info("🔍 [Admin] Попытка активации клиента с ID: {}", clientId);
        
        ClientApplication client = clientAppRepository.findById(clientId)
            .orElseThrow(() -> {
                log.error("❌ [Admin] Клиент с ID {} не найден", clientId);
                return new IllegalArgumentException("Client not found");
            });
        
        log.info("📋 [Admin] Найден клиент: {} (активен: {})", client.getName(), client.getIsActive());
        
        if (client.getIsActive()) {
            log.warn("⚠️ [Admin] Клиент {} уже активен", client.getName());
            return;
        }
        
        client.setIsActive(true);
        ClientApplication savedClient = clientAppRepository.save(client);
        
        log.info("✅ [Admin] Клиент {} успешно активирован (ID: {})", savedClient.getName(), savedClient.getId());
    }
    
    /**
     * Регенерировать API ключ
     */
    @Transactional
    @SuppressWarnings("null")
    public ClientAppDTO regenerateApiKey(@NotNull UUID id) {
        UUID clientId = requireClientId(id);
        ClientApplication client = clientAppRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        
        client.setApiKey(generateApiKey());
        client = clientAppRepository.save(client);
        return toDTO(client);
    }

    /**
     * Привязать клиентское приложение к пользователю
     */
    @Transactional
    @SuppressWarnings("null")
    public ClientAppDTO assignClientToUser(@NotNull UUID clientId, AssignClientUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        UUID targetClientId = requireClientId(clientId);
        ClientApplication client = clientAppRepository.findById(targetClientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        UserAccount user = resolveTargetUser(request);

        UserClientLink link = userClientLinkRepository.findByClientApplication(targetClientId)
            .orElseGet(UserClientLink::new);
        link.setClientApplication(client);
        link.setUser(user);
        userClientLinkRepository.save(link);

        log.info("🔗 [Admin] Клиент {} привязан к пользователю {}", client.getName(), user.getEmail());
        return toDTO(client);
    }

    @SuppressWarnings("null")
    private UserAccount resolveTargetUser(AssignClientUserRequest request) {
        if (request.getUserId() != null) {
            return userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));
        }

        String email = request.normalizedEmail();
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("userEmail is required when userId is not provided");
        }

        Optional<UserAccount> existing = userAccountRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (!request.isCreateUserIfMissing()) {
            throw new IllegalArgumentException("User not found for email " + email);
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setFullName(
            StringUtils.hasText(request.getUserFullName())
                ? request.getUserFullName().trim()
                : email
        );
        user.setProvider("admin-link");
        user.setActive(true);
        UserAccount saved = userAccountRepository.save(user);
        log.info("👤 [Admin] Создан новый пользователь {} для привязки клиента", email);
        return saved;
    }

    @NotNull
    private UUID requireClientId(UUID id) {
        return Objects.requireNonNull(id, "Client id is required");
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

