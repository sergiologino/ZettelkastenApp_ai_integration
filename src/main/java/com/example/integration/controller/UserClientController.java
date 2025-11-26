package com.example.integration.controller;

import com.example.integration.dto.user.ClientApplicationDto;
import com.example.integration.dto.user.ClientCreateRequest;
import com.example.integration.dto.user.ClientUpdateRequest;
import com.example.integration.dto.user.NetworkUsageStatsDto;
import com.example.integration.dto.user.SetClientNetworksRequest;
import com.example.integration.service.UserClientService;
import com.example.integration.service.UserContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Client Applications", description = "Управление клиентскими приложениями конкретного пользователя (реферальные ключи, доступ к сетям). Требуется пользовательская авторизация.")
public class UserClientController {

    private final UserContextService userContextService;
    private final UserClientService userClientService;

    public UserClientController(UserContextService userContextService, UserClientService userClientService) {
        this.userContextService = userContextService;
        this.userClientService = userClientService;
    }

    @Operation(summary = "Список клиентских приложений", description = "Возвращает все приложения, созданные текущим пользователем, вместе с API-ключами и статусом.")
    @GetMapping("/clients")
    public ResponseEntity<?> listClients(HttpServletRequest request) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(userClientService.list(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @Operation(
            summary = "Создать клиентское приложение",
            description = "Создаёт новое приложение и автоматически привязывает его к текущему пользователю.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ClientCreateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "noteapp",
                                      "description": "Prod integration for AltaNote",
                                      "allowedIps": ["31.173.24.10/32"],
                                      "webhookUrl": "https://noteapp.ru/api/ai/webhook"
                                    }
                                    """)
                    )
            )
    )
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

    @Operation(summary = "Обновить параметры клиента", description = "Позволяет изменить описание, IP-белый список, webhook и др. для указанного клиента.")
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

    @Operation(summary = "Удалить клиентское приложение", description = "Удаляет приложение и отвязывает все связанные ключи/сети.")
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

    @Operation(summary = "Сгенерировать новый API-ключ", description = "Пересоздаёт секрет для приложения и возвращает свежий ключ.")
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

    @Operation(summary = "Список активных нейросетей", description = "Возвращает все нейросети, которые сейчас доступны пользователю для подключения к своим приложениям.")
    @GetMapping("/networks/available")
    public ResponseEntity<?> availableNetworks() {
        return ResponseEntity.ok(userClientService.getAvailableNetworks());
    }

    @Operation(
            summary = "Назначить нейросети клиенту",
            description = "Управляет доступом клиента к сетям. Можно передать простой список `networkIds` или расширенный `networks` с приоритетом.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SetClientNetworksRequest.class),
                            examples = {
                                    @ExampleObject(name = "Простой формат", value = """
                                            {
                                              "networkIds": [
                                                "2d8f6a7a-8b5d-45c9-9df3-3c0c1cf6d1ef",
                                                "97d3b52c-12e0-42c8-90c0-27cf66a9c607"
                                              ]
                                            }
                                            """),
                                    @ExampleObject(name = "Расширенный формат", value = """
                                            {
                                              "networks": [
                                                {"networkId": "2d8f6a7a-8b5d-45c9-9df3-3c0c1cf6d1ef", "priority": 10},
                                                {"networkId": "97d3b52c-12e0-42c8-90c0-27cf66a9c607", "priority": 5}
                                              ]
                                            }
                                            """)
                            }
                    )
            )
    )
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

    @Operation(summary = "Список сетей клиента", description = "Возвращает текущий набор нейросетей, назначенных клиенту, включая приоритеты.")
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

    @Operation(summary = "Статистика по сетям клиента", description = "Обороты запросов/токенов по каждой нейросети для выбранного клиента.")
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


