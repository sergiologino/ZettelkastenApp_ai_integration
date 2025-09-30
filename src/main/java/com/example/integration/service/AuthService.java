package com.example.integration.service;

import com.example.integration.dto.AdminAuthResponse;
import com.example.integration.dto.AdminLoginRequest;
import com.example.integration.model.AdminUser;
import com.example.integration.repository.AdminUserRepository;
import com.example.integration.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис аутентификации администраторов
 */
@Service
public class AuthService {
    
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
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        
        if (!admin.getIsActive()) {
            throw new IllegalArgumentException("Account is disabled");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        
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

