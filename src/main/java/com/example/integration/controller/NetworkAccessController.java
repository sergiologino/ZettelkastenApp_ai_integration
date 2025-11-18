package com.example.integration.controller;

import com.example.integration.dto.ClientNetworkAccessDTO;
import com.example.integration.dto.GrantAccessRequest;
import com.example.integration.service.NetworkAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер для управления доступом клиентов к нейросетям (только для администраторов)
 */
@RestController
@RequestMapping("/api/admin/access")
@Tag(name = "Network Access Management", description = "Управление доступом клиентов к нейросетям")
public class NetworkAccessController {

    private static final Logger log = LoggerFactory.getLogger(NetworkAccessController.class);

    private final NetworkAccessService networkAccessService;

    @Autowired
    public NetworkAccessController(NetworkAccessService networkAccessService) {
        this.networkAccessService = networkAccessService;
    }

    /**
     * Получить все доступы
     */
    @GetMapping
    @Operation(summary = "Получить все доступы", description = "Получить список всех доступов клиентов к нейросетям")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список доступов получен успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<List<ClientNetworkAccessDTO>> getAllAccesses() {
        log.info("🔍 [Admin] Получение всех доступов клиентов к нейросетям");
        
        try {
            List<ClientNetworkAccessDTO> accesses = networkAccessService.getAllAccesses();
            log.info("✅ [Admin] Получено {} доступов", accesses.size());
            return ResponseEntity.ok(accesses);
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка получения доступов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить доступ по ID
     */
    @GetMapping("/{accessId}")
    @Operation(summary = "Получить доступ по ID", description = "Получить информацию о конкретном доступе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доступ найден"),
            @ApiResponse(responseCode = "404", description = "Доступ не найден"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<ClientNetworkAccessDTO> getAccessById(@PathVariable UUID accessId) {
        log.info("🔍 [Admin] Получение доступа по ID: {}", accessId);
        
        try {
            ClientNetworkAccessDTO access = networkAccessService.getAccessById(accessId);
            log.info("✅ [Admin] Доступ найден: {} -> {}", access.getClientName(), access.getNetworkDisplayName());
            return ResponseEntity.ok(access);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Admin] Доступ не найден: {}", accessId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка получения доступа", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Предоставить доступ клиенту КО ВСЕМ активным нейросетям
     */
    @PostMapping("/grant-all/{clientId}")
    @Operation(summary = "Предоставить доступ ко всем сетям", description = "Автоматически предоставить доступ клиенту ко всем активным нейросетям")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доступ предоставлен успешно"),
            @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<?> grantAccessToAllNetworks(@PathVariable UUID clientId) {
        log.info("🔗 [Admin] ===== Предоставление доступа клиенту {} ко ВСЕМ активным нейросетям =====", clientId);
        
        try {
            java.util.Map<String, Object> result = networkAccessService.grantAccessToAllNetworks(clientId);
            log.info("✅ [Admin] Доступ предоставлен к {} нейросетям", result.get("granted"));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Admin] Ошибка: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка предоставления доступа", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Ошибка предоставления доступа: " + e.getMessage()));
        }
    }
    
    /**
     * Предоставить доступ клиенту к нейросети
     */
    @PostMapping
    @Operation(summary = "Предоставить доступ", description = "Предоставить или обновить доступ клиента к нейросети")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доступ предоставлен успешно"),
            @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<ClientNetworkAccessDTO> grantAccess(@RequestBody GrantAccessRequest request) {
        log.info("🔗 [Admin] Предоставление доступа клиенту {} к нейросети {}", 
                request.getClientId(), request.getNetworkId());
        
        try {
            ClientNetworkAccessDTO access = networkAccessService.grantAccess(request);
            log.info("✅ [Admin] Доступ предоставлен: {} -> {} (лимиты: {})", 
                    access.getClientName(), access.getNetworkDisplayName(), access.getLimitsDescription());
            return ResponseEntity.ok(access);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Admin] Ошибка валидации: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка предоставления доступа", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Отозвать доступ
     */
    @DeleteMapping("/{accessId}")
    @Operation(summary = "Отозвать доступ", description = "Отозвать доступ клиента к нейросети")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доступ отозван успешно"),
            @ApiResponse(responseCode = "404", description = "Доступ не найден"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<Void> revokeAccess(@PathVariable UUID accessId) {
        log.info("🗑️ [Admin] Отзыв доступа с ID: {}", accessId);
        
        try {
            networkAccessService.revokeAccess(accessId);
            log.info("✅ [Admin] Доступ отозван успешно");
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Admin] Доступ не найден: {}", accessId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка отзыва доступа", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить доступы для конкретного клиента
     */
    @GetMapping("/client/{clientId}")
    @Operation(summary = "Получить доступы клиента", description = "Получить все доступы конкретного клиента")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доступы получены успешно"),
            @ApiResponse(responseCode = "404", description = "Клиент не найден"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<List<ClientNetworkAccessDTO>> getClientAccesses(@PathVariable UUID clientId) {
        log.info("🔍 [Admin] Получение доступов для клиента: {}", clientId);
        
        try {
            List<ClientNetworkAccessDTO> accesses = networkAccessService.getClientAccesses(clientId);
            log.info("✅ [Admin] Получено {} доступов для клиента {}", accesses.size(), clientId);
            return ResponseEntity.ok(accesses);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Admin] Клиент не найден: {}", clientId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка получения доступов клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить доступы для конкретной нейросети
     */
    @GetMapping("/network/{networkId}")
    @Operation(summary = "Получить доступы нейросети", description = "Получить всех клиентов с доступом к конкретной нейросети")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доступы получены успешно"),
            @ApiResponse(responseCode = "404", description = "Нейросеть не найдена"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<List<ClientNetworkAccessDTO>> getNetworkAccesses(@PathVariable UUID networkId) {
        log.info("🔍 [Admin] Получение доступов для нейросети: {}", networkId);
        
        try {
            List<ClientNetworkAccessDTO> accesses = networkAccessService.getNetworkAccesses(networkId);
            log.info("✅ [Admin] Получено {} доступов для нейросети {}", accesses.size(), networkId);
            return ResponseEntity.ok(accesses);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [Admin] Нейросеть не найдена: {}", networkId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка получения доступов нейросети", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить статистику доступов
     */
    @GetMapping("/stats")
    @Operation(summary = "Получить статистику доступов", description = "Получить общую статистику по доступам")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика получена успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<NetworkAccessService.AccessStats> getAccessStats() {
        log.info("📊 [Admin] Получение статистики доступов");
        
        try {
            NetworkAccessService.AccessStats stats = networkAccessService.getAccessStats();
            log.info("✅ [Admin] Статистика получена: {} всего, {} с лимитами, {} неограниченных", 
                    stats.getTotalAccesses(), stats.getAccessesWithLimits(), stats.getUnlimitedAccesses());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка получения статистики", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить доступы, сгруппированные по пользователям: Пользователь → Сервисы → Нейросети
     */
    @GetMapping("/grouped")
    @Operation(summary = "Получить группированные доступы", description = "Получить доступы, сгруппированные по пользователям, сервисам и нейросетям")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доступы получены успешно"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    public ResponseEntity<List<com.example.integration.dto.UserAccessGroupDto>> getGroupedAccesses() {
        log.info("📊 [Admin] Получение группированных доступов");
        
        try {
            List<com.example.integration.dto.UserAccessGroupDto> grouped = networkAccessService.getGroupedAccesses();
            log.info("✅ [Admin] Получено {} групп пользователей", grouped.size());
            return ResponseEntity.ok(grouped);
        } catch (Exception e) {
            log.error("❌ [Admin] Ошибка получения группированных доступов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
