package com.example.integration.security;

import com.example.integration.repository.ClientApplicationRepository;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final ClientApplicationRepository clientApplicationRepository;
    
    public SecurityConfig(ClientApplicationRepository clientApplicationRepository) {
        this.clientApplicationRepository = clientApplicationRepository;
        log.warn("========================================");
        log.warn("🔧 SecurityConfig ЗАГРУЖЕН!");
        log.warn("✅ API Key фильтр будет подключен!");
        log.warn("========================================");
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("🔑 Создан PasswordEncoder bean");
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.warn("🔒 Настройка SecurityFilterChain с API Key фильтром");
        
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // ✅ ИСПРАВЛЕНИЕ: Добавляем API Key фильтр ПЕРЕД стандартной аутентификацией
            .addFilterBefore(new ApiKeyAuthFilter(clientApplicationRepository), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Публичные endpoints
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/api/auth/**",
                    "/api/admin/**"  // Админские endpoints (с JWT)
                ).permitAll()
                // Клиентские AI endpoints требуют X-API-Key
                .requestMatchers("/api/ai/**").authenticated()
                // Все остальное запрещено
                .anyRequest().denyAll()
            );
        
        log.warn("✅ SecurityFilterChain настроен - API Key фильтр включен");
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

