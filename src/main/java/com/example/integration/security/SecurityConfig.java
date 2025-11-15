package com.example.integration.security;

import com.example.integration.repository.ClientApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    private final JwtAuthFilter jwtAuthFilter;
    private static final List<String> FRONTEND_ORIGINS = List.of(
        "https://sergiologino-ai-integration-front-cd2e.twc1.net"
    );
    
    public SecurityConfig(
        ClientApplicationRepository clientApplicationRepository,
        JwtAuthFilter jwtAuthFilter
    ) {
        this.clientApplicationRepository = clientApplicationRepository;
        this.jwtAuthFilter = jwtAuthFilter;
        log.warn("========================================");
        log.warn("🔧 SecurityConfig ЗАГРУЖЕН!");
        log.warn("✅ JWT и API Key фильтры будут подключены!");
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
            // Поддерживаем авторизацию по JWT и X-API-Key
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new ApiKeyAuthFilter(clientApplicationRepository), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Публичные endpoints
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // Админские endpoints доступны только с JWT
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Клиентские AI endpoints требуют X-API-Key (авторизацию настраивает ApiKey фильтр)
                .requestMatchers("/api/ai/**").authenticated()
                // Все остальное запрещено
                .anyRequest().denyAll()
            );
        
        log.warn("✅ SecurityFilterChain настроен - JWT и API Key фильтры включены");
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("🌐 Настройка CORS - разрешены все домены");
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(FRONTEND_ORIGINS);
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

