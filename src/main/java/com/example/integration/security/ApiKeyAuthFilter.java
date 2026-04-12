package com.example.integration.security;

import com.example.integration.model.ClientApplication;
import com.example.integration.repository.ClientApplicationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class ApiKeyAuthFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    
    private final ClientApplicationRepository clientAppRepository;
    
    public ApiKeyAuthFilter(ClientApplicationRepository clientAppRepository) {
        this.clientAppRepository = clientAppRepository;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        boolean isNetworksEndpoint = requestURI != null && requestURI.contains("/networks/available");
        boolean isAiProcessEndpoint = requestURI != null && requestURI.contains("/api/ai/process");
        boolean shouldLog = isNetworksEndpoint || isAiProcessEndpoint;
        
        if (shouldLog) {
            log.info("🔵 [ApiKeyAuthFilter] Обработка запроса: {}", requestURI);
        }

        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            if (shouldLog) {
                log.info("🔍 [ApiKeyAuthFilter] Bearer токен присутствует, пропускаем X-API-Key аутентификацию");
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (requestURI != null && requestURI.startsWith("/api/admin/")) {
            if (shouldLog) {
                log.info("🔍 [ApiKeyAuthFilter] Админский endpoint, пропускаем X-API-Key аутентификацию");
            }
            filterChain.doFilter(request, response);
            return;
        }
        
        String apiKey = request.getHeader("X-API-Key");
        
        if (shouldLog) {
            log.info("🔍 [ApiKeyAuthFilter] X-API-Key header: {}", apiKey != null && !apiKey.isEmpty() ? "присутствует" : "отсутствует");
            if (apiKey != null && !apiKey.isEmpty()) {
                log.info("🔍 [ApiKeyAuthFilter] API Key длина: {}", apiKey.length());
                log.info("🔍 [ApiKeyAuthFilter] API Key preview: {}", apiKey.substring(0, Math.min(20, apiKey.length())) + "...");
            }
        }
        
        if (apiKey != null && !apiKey.isEmpty()) {
            Optional<ClientApplication> clientApp = clientAppRepository.findByApiKey(apiKey);
            
            if (clientApp.isPresent() && clientApp.get().getIsActive()) {
                if (shouldLog) {
                    log.info("✅ [ApiKeyAuthFilter] Клиент найден: {} (ID: {})", 
                        clientApp.get().getName(), clientApp.get().getId());
                }
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        clientApp.get(), 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"))
                    );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                if (shouldLog) {
                    if (clientApp.isEmpty()) {
                        log.warn("⚠️ [ApiKeyAuthFilter] Клиент с таким API ключом не найден");
                    } else if (!clientApp.get().getIsActive()) {
                        log.warn("⚠️ [ApiKeyAuthFilter] Клиент найден, но неактивен: {}", clientApp.get().getName());
                    }
                }
            }
        } else {
            if (shouldLog) {
                log.warn("⚠️ [ApiKeyAuthFilter] X-API-Key заголовок отсутствует или пуст");
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

