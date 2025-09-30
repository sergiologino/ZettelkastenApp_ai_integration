package com.example.integration.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фильтр для ограничения количества запросов с одного IP адреса
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    // Кэш bucket'ов для каждого IP адреса
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    // Настройки: 100 запросов в минуту с одного IP
    private final int REQUESTS_PER_MINUTE = 100;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Пропускаем health check и swagger
        String path = request.getRequestURI();
        if (path.contains("/actuator/") || path.contains("/swagger") || path.contains("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String ip = getClientIP(request);
        Bucket bucket = resolveBucket(ip);
        
        if (bucket.tryConsume(1)) {
            // Запрос разрешён
            filterChain.doFilter(request, response);
        } else {
            // Превышен лимит запросов
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Too many requests\", \"message\": \"Rate limit exceeded. Max " 
                + REQUESTS_PER_MINUTE + " requests per minute per IP.\"}"
            );
        }
    }
    
    /**
     * Получить или создать bucket для IP адреса
     */
    private Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, k -> createNewBucket());
    }
    
    /**
     * Создать новый bucket с настройками rate limiting
     */
    private Bucket createNewBucket() {
        // Создаём ограничение: REQUESTS_PER_MINUTE запросов в минуту
        Bandwidth limit = Bandwidth.classic(
            REQUESTS_PER_MINUTE,
            Refill.intervally(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
        );
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Получить реальный IP клиента (с учётом прокси/балансировщика)
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        // X-Forwarded-For может содержать несколько IP через запятую
        return xfHeader.split(",")[0];
    }
}

