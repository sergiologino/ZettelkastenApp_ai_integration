package com.example.integration.service;

import com.example.integration.dto.user.ClientApplicationDto;
import com.example.integration.dto.user.ClientCreateRequest;
import com.example.integration.dto.user.ClientUpdateRequest;
import com.example.integration.model.ClientApplication;
import com.example.integration.model.UserAccount;
import com.example.integration.model.UserClientLink;
import com.example.integration.repository.ClientApplicationRepository;
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

    public UserClientService(ClientApplicationRepository clientApplicationRepository,
                             UserClientLinkRepository linkRepository) {
        this.clientApplicationRepository = clientApplicationRepository;
        this.linkRepository = linkRepository;
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
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        return dto;
    }

    private String generateApiKey() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }
}


