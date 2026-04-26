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
import jakarta.servlet.http.HttpServletResponse;

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
                // Публичные endpoints (должны быть ПЕРЕД более общими правилами)
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // Auth endpoints - КРИТИЧНО: должны быть публичными и проверяться ДО /api/user/**
                // Используем явные пути для избежания конфликтов
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/user/auth/register").permitAll()
                .requestMatchers("/api/user/auth/login").permitAll()
                .requestMatchers("/api/user/auth/oauth2/authorize/**").permitAll()
                .requestMatchers("/api/user/auth/oauth2/callback/**").permitAll()
                .requestMatchers("/api/user/auth/oauth2/**").permitAll()
                .requestMatchers("/api/user/auth/**").permitAll()
                // OAuth callback от провайдеров (Google/Yandex редиректят сюда)
                .requestMatchers("/login/oauth2/code/**").permitAll()
                .requestMatchers("/login/**").permitAll()
                // Админские endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Пользовательский кабинет - только специфичные пути (auth уже обработан выше)
                // НЕ используем общее правило /api/user/** чтобы избежать конфликтов с /api/user/auth/**
                .requestMatchers("/api/user/clients/**").hasAnyRole("USER","ADMIN")
                .requestMatchers("/api/user/networks/**").hasAnyRole("USER","ADMIN")
                .requestMatchers("/api/user/subscriptions/**").hasAnyRole("USER","ADMIN")
                .requestMatchers("/api/user/api-keys/**").hasAnyRole("USER","ADMIN")
                // Webhook от платежных провайдеров (публичный, но должен быть защищен на уровне провайдера)
                .requestMatchers("/api/payments/webhook/**").permitAll()
                // Если появятся новые пути в /api/user/, добавляйте их здесь явно
                // Клиентские endpoints требуют X-API-Key (авторизацию настраивает ApiKeyAuthFilter)
                .requestMatchers("/api/ai/**").authenticated()
                .requestMatchers("/api/social/**").authenticated()
                // Все остальное запрещено
                .anyRequest().denyAll()
            )
            // Добавляем обработчик для логирования доступа
            .exceptionHandling(ex -> {
                ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.error("❌ [SecurityConfig] Доступ запрещен для {} {}: {}", 
                        request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());
                    org.springframework.security.core.Authentication auth = 
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null) {
                        log.error("   Principal: {}, Authorities: {}", 
                            auth.getPrincipal().getClass().getName(), auth.getAuthorities());
                    } else {
                        log.error("   Authentication отсутствует!");
                    }
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                });
            });
        
        log.warn("✅ SecurityFilterChain настроен - JWT и API Key фильтры включены");
        log.warn("🔓 Публичные эндпоинты: /api/auth/**, /api/user/auth/**, /login/**");
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

