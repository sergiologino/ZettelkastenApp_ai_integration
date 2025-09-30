package com.example.integration.controller;

import com.example.integration.dto.AdminAuthResponse;
import com.example.integration.dto.AdminLoginRequest;
import com.example.integration.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API для аутентификации администраторов
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication API for administrators")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * Вход администратора
     */
    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate administrator and get JWT token")
    public ResponseEntity<AdminAuthResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminAuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Регистрация первого администратора (только если нет других админов)
     */
    @PostMapping("/register")
    @Operation(summary = "Register first admin", description = "Register the first administrator (only if no admins exist)")
    public ResponseEntity<AdminAuthResponse> register(@Valid @RequestBody AdminLoginRequest request) {
        // TODO: проверить, что это первый админ
        AdminAuthResponse response = authService.register(
            request.getUsername(), 
            request.getUsername() + "@example.com", 
            request.getPassword()
        );
        return ResponseEntity.ok(response);
    }
}

