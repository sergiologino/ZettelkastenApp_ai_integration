package com.example.integration.controller;

import com.example.integration.dto.AdminAuthResponse;
import com.example.integration.dto.AdminLoginRequest;
import com.example.integration.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request) {
        try {
            AdminAuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            error.put("status", 400);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Регистрация первого администратора (только если нет других админов)
     */
    @PostMapping("/register")
    @Operation(summary = "Register first admin", description = "Register the first administrator (only if no admins exist)")
    public ResponseEntity<?> register(@Valid @RequestBody AdminLoginRequest request) {
        try {
            AdminAuthResponse response = authService.register(
                request.getUsername(), 
                request.getUsername() + "@example.com", 
                request.getPassword()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            error.put("status", 400);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

