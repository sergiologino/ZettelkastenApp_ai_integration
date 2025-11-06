package com.example.integration.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    
    public SecurityConfig() {
        log.warn("========================================");
        log.warn("🔧 SecurityConfig ЗАГРУЖЕН!");
        log.warn("⚠️ ВНИМАНИЕ: Временная конфигурация без авторизации!");
        log.warn("========================================");
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("🔑 Создан PasswordEncoder bean");
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.warn("🔒 Настройка SecurityFilterChain - ВСЕ ENDPOINTS ОТКРЫТЫ");
        
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ⚠️ ВРЕМЕННО: Разрешаем ВСЕ запросы для отладки CORS
                .anyRequest().permitAll()
            );
        
        log.warn("✅ SecurityFilterChain настроен - CORS включен, авторизация ОТКЛЮЧЕНА");
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("🌐 Настройка CORS - разрешены все домены");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ Для разработки - разрешаем все домены
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-API-Key", "Content-Type"));
        configuration.setAllowCredentials(true); // Разрешаем credentials для Swagger UI
        configuration.setMaxAge(3600L); // Кэшируем preflight на 1 час
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("✅ CORS настроен успешно");
        return source;
    }
}

