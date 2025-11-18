package com.example.integration.controller;

import com.example.integration.dto.user.ClientApplicationDto;
import com.example.integration.dto.user.ClientCreateRequest;
import com.example.integration.dto.user.ClientUpdateRequest;
import com.example.integration.dto.user.NetworkUsageStatsDto;
import com.example.integration.dto.user.SetClientNetworksRequest;
import com.example.integration.service.UserClientService;
import com.example.integration.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserClientController {

    private final UserContextService userContextService;
    private final UserClientService userClientService;

    public UserClientController(UserContextService userContextService, UserClientService userClientService) {
        this.userContextService = userContextService;
        this.userClientService = userClientService;
    }

    @GetMapping("/clients")
    public ResponseEntity<?> listClients(HttpServletRequest request) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(userClientService.list(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/clients")
    public ResponseEntity<?> createClient(HttpServletRequest request, @RequestBody ClientCreateRequest req) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        ClientApplicationDto dto = userClientService.create(user, req);
                        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<?> updateClient(HttpServletRequest request, @PathVariable("id") UUID id, @RequestBody ClientUpdateRequest req) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        ClientApplicationDto dto = userClientService.update(user, id, req);
                        return ResponseEntity.ok(dto);
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<?> deleteClient(HttpServletRequest request, @PathVariable("id") UUID id) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        userClientService.delete(user, id);
                        return ResponseEntity.noContent().build();
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/clients/{id}/regenerate-key")
    public ResponseEntity<?> regenerateKey(HttpServletRequest request, @PathVariable("id") UUID id) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        ClientApplicationDto dto = userClientService.regenerateKey(user, id);
                        return ResponseEntity.ok(dto);
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Получить список доступных активных нейросетей
     */
    @GetMapping("/networks/available")
    public ResponseEntity<?> availableNetworks() {
        return ResponseEntity.ok(userClientService.getAvailableNetworks());
    }

    /**
     * Установить список подключенных нейросетей для клиента
     */
    @PutMapping("/clients/{id}/networks")
    public ResponseEntity<?> setClientNetworks(HttpServletRequest request, 
                                                @PathVariable("id") UUID id,
                                                @RequestBody SetClientNetworksRequest req) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        // Поддерживаем оба формата: старый (networkIds) и новый (networks с приоритетами)
                        if (req.getNetworks() != null && !req.getNetworks().isEmpty()) {
                            // Новый формат с приоритетами
                            userClientService.setClientNetworksWithPriority(user, id, req.getNetworks());
                        } else if (req.getNetworkIds() != null && !req.getNetworkIds().isEmpty()) {
                            // Старый формат для обратной совместимости
                            userClientService.setClientNetworks(user, id, req.getNetworkIds());
                        } else {
                            return ResponseEntity.badRequest().body(Map.of("error", "networkIds или networks обязателен"));
                        }
                        return ResponseEntity.noContent().build();
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Получить список подключенных нейросетей для клиента с приоритетами
     */
    @GetMapping("/clients/{id}/networks")
    public ResponseEntity<?> getClientNetworks(HttpServletRequest request, @PathVariable("id") UUID id) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        List<com.example.integration.dto.user.ClientNetworkAccessDto> networks = 
                            userClientService.getClientNetworksWithPriority(user, id);
                        return ResponseEntity.ok(networks);
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Получить статистику использования нейросетей для клиента
     */
    @GetMapping("/clients/{id}/networks/stats")
    public ResponseEntity<?> getNetworkStats(HttpServletRequest request, @PathVariable("id") UUID id) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        List<NetworkUsageStatsDto> stats = userClientService.getNetworkUsageStats(user, id);
                        return ResponseEntity.ok(stats);
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}


