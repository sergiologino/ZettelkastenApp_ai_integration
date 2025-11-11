package com.example.integration.service;

import com.example.integration.dto.AdminAuthResponse;
import com.example.integration.dto.AdminLoginRequest;
import com.example.integration.model.AdminUser;
import com.example.integration.repository.AdminUserRepository;
import com.example.integration.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис аутентификации администраторов
 */
@Service
public class AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public AuthService(
        AdminUserRepository adminUserRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Аутентификация администратора
     */
    @Transactional(readOnly = true)
    public AdminAuthResponse login(AdminLoginRequest request) {
        log.info("🔐 Попытка входа: username={}", request.getUsername());
        
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> {
                log.warn("❌ Неудачная попытка входа: пользователь не найден");
                return new IllegalArgumentException("Invalid credentials");
            });
        
        if (!admin.getIsActive()) {
            log.warn("❌ Попытка входа с деактивированным аккаунтом: {}", request.getUsername());
            throw new IllegalArgumentException("Account is disabled");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            log.warn("❌ Неудачная попытка входа: неверный пароль для {}", request.getUsername());
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        log.info("✅ Успешный вход: username={}", request.getUsername());
        String token = jwtUtil.generateToken(admin.getUsername());
        
        return new AdminAuthResponse(token, admin.getUsername(), admin.getEmail());
    }
    
    /**
     * Регистрация нового администратора (только для первичной настройки)
     */
    @Transactional
    public AdminAuthResponse register(String username, String email, String password) {
        if (adminUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        if (adminUserRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setIsActive(true);
        
        adminUserRepository.save(admin);
        
        String token = jwtUtil.generateToken(admin.getUsername());
        
        return new AdminAuthResponse(token, admin.getUsername(), admin.getEmail());
    }
    
}

