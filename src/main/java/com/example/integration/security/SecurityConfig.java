package com.example.integration.security;

import com.example.integration.repository.AdminUserRepository;
import com.example.integration.repository.ClientApplicationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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
    
    private final RateLimitFilter rateLimitFilter;
    
    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }
    
    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil, @Lazy AdminUserRepository adminUserRepository) {
        return new JwtAuthFilter(jwtUtil, adminUserRepository);
    }
    
    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter(@Lazy ClientApplicationRepository clientApplicationRepository) {
        return new ApiKeyAuthFilter(clientApplicationRepository);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, 
                                           JwtAuthFilter jwtAuthFilter, 
                                           ApiKeyAuthFilter apiKeyAuthFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ✅ Публичные эндпоинты (БЕЗ аутентификации)
                .requestMatchers("/api/auth/**").permitAll()  // Все auth эндпоинты
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/error").permitAll()
                
                // AI API - требует API-ключ клиентского приложения
                .requestMatchers("/api/ai/**").hasRole("CLIENT")
                
                // Admin API - требует JWT токен админа
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Все остальные запросы требуют аутентификации
                .anyRequest().authenticated()
            )
            // Фильтры: Rate Limit → API Key → JWT
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
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
        return source;
    }
}

