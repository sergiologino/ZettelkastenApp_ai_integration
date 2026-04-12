package com.example.integration.controller;

import com.example.integration.dto.user.SaveUserApiKeyRequest;
import com.example.integration.dto.user.UserApiKeyDto;
import com.example.integration.model.UserAccount;
import com.example.integration.model.UserApiKey;
import com.example.integration.service.UserApiKeyService;
import com.example.integration.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/api-keys")
@SecurityRequirement(name = "Bearer")
public class UserApiKeyController {
    
    private final UserApiKeyService userApiKeyService;
    private final UserContextService userContextService;
    
    public UserApiKeyController(
            UserApiKeyService userApiKeyService,
            UserContextService userContextService) {
        this.userApiKeyService = userApiKeyService;
        this.userContextService = userContextService;
    }
    
    /**
     * Сохранить или обновить пользовательский API ключ
     */
    @PostMapping
    public ResponseEntity<?> saveApiKey(HttpServletRequest request,
                                        @Valid @RequestBody SaveUserApiKeyRequest req) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        // Проверяем платный тариф
                        if (!userApiKeyService.hasPaidSubscription(user)) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "Использование собственных API ключей доступно только для платного тарифа"));
                        }
                        
                        UserApiKey userApiKey = userApiKeyService.saveApiKey(
                                user, req.getClientId(), req.getNetworkId(), req.getApiKey());
                        
                        return ResponseEntity.ok(toDto(userApiKey));
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
    
    /**
     * Получить все API ключи пользователя для конкретного клиента
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getApiKeysByClient(HttpServletRequest request,
                                                 @PathVariable UUID clientId) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        List<UserApiKey> keys = userApiKeyService.getUserApiKeys(user, clientId);
                        List<UserApiKeyDto> dtos = keys.stream()
                                .map(this::toDto)
                                .collect(Collectors.toList());
                        return ResponseEntity.ok(dtos);
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
    
    /**
     * Получить все API ключи пользователя
     */
    @GetMapping
    public ResponseEntity<?> getAllApiKeys(HttpServletRequest request) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    List<UserApiKey> keys = userApiKeyService.getAllUserApiKeys(user);
                    List<UserApiKeyDto> dtos = keys.stream()
                            .map(this::toDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(dtos);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
    
    /**
     * Удалить пользовательский API ключ
     */
    @DeleteMapping("/client/{clientId}/network/{networkId}")
    public ResponseEntity<?> deleteApiKey(HttpServletRequest request,
                                          @PathVariable UUID clientId,
                                          @PathVariable UUID networkId) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        userApiKeyService.deleteApiKey(user, clientId, networkId);
                        return ResponseEntity.noContent().build();
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
    
    /**
     * Проверить, имеет ли пользователь доступ к функции пользовательских ключей
     */
    @GetMapping("/check-access")
    public ResponseEntity<?> checkAccess(HttpServletRequest request) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    boolean hasAccess = userApiKeyService.hasPaidSubscription(user);
                    return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
    
    private UserApiKeyDto toDto(UserApiKey userApiKey) {
        UserApiKeyDto dto = new UserApiKeyDto();
        dto.setId(userApiKey.getId());
        dto.setClientApplicationId(userApiKey.getClientApplication().getId());
        dto.setClientApplicationName(userApiKey.getClientApplication().getName());
        dto.setNeuralNetworkId(userApiKey.getNeuralNetwork().getId());
        dto.setNeuralNetworkName(userApiKey.getNeuralNetwork().getName());
        dto.setNeuralNetworkDisplayName(userApiKey.getNeuralNetwork().getDisplayName());
        dto.setCreatedAt(userApiKey.getCreatedAt());
        dto.setUpdatedAt(userApiKey.getUpdatedAt());
        dto.setHasKey(true); // Если объект существует, значит ключ установлен
        return dto;
    }
}

