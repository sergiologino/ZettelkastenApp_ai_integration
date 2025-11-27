package com.example.integration.security;

import com.example.integration.repository.AdminUserRepository;
import com.example.integration.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    private final JwtUtil jwtUtil;
    private final AdminUserRepository adminUserRepository;
    private final UserAccountRepository userAccountRepository;

    public JwtAuthFilter(JwtUtil jwtUtil,
                         AdminUserRepository adminUserRepository,
                         UserAccountRepository userAccountRepository) {
        this.jwtUtil = jwtUtil;
        this.adminUserRepository = adminUserRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("🔍 [JwtAuthFilter] ===== Обработка запроса: {} {} =====", request.getMethod(), path);

        String jwt = getJwtFromRequest(request);
        if (!StringUtils.hasText(jwt)) {
            log.info("⚠️ [JwtAuthFilter] JWT токен отсутствует в заголовке Authorization");
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtValid(jwt)) {
            log.warn("⚠️ [JwtAuthFilter] JWT токен невалиден");
            filterChain.doFilter(request, response);
            return;
        }

        String subject = jwtUtil.getUsernameFromToken(jwt);
        log.info("✅ [JwtAuthFilter] Извлечен subject из токена: {}", subject);

        // Try admin (by username first, then by email)
        var adminOpt = adminUserRepository.findByUsername(subject);
        if (adminOpt.isEmpty()) {
            // Если не найден по username, пробуем по email
            adminOpt = adminUserRepository.findByEmail(subject);
        }
        
        if (adminOpt.isPresent() && Boolean.TRUE.equals(adminOpt.get().getIsActive())) {
            log.info("✅ [JwtAuthFilter] Найден активный админ: {} (username: {})", subject, adminOpt.get().getUsername());
            var auth = new UsernamePasswordAuthenticationToken(
                    adminOpt.get(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("✅ [JwtAuthFilter] Роль ROLE_ADMIN установлена в SecurityContext. Authorities: {}", auth.getAuthorities());
        } else {
            // Fallback to end-user by email
            log.info("🔍 [JwtAuthFilter] Админ не найден, ищем пользователя по email: {}", subject);
            var userOpt = userAccountRepository.findByEmail(subject);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                if (user.isActive()) {
                    log.info("✅ [JwtAuthFilter] Найден активный пользователь: {} (email: {})", subject, user.getEmail());
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("✅ [JwtAuthFilter] Роль ROLE_USER установлена в SecurityContext. Authorities: {}", auth.getAuthorities());
                } else {
                    log.warn("⚠️ [JwtAuthFilter] Пользователь {} найден, но неактивен", subject);
                }
            } else {
                log.warn("⚠️ [JwtAuthFilter] Пользователь {} не найден ни в админах, ни в пользователях", subject);
            }
        }

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            log.info("✅ [JwtAuthFilter] Текущая аутентификация в SecurityContext: principal={}, authorities={}", 
                currentAuth.getPrincipal().getClass().getSimpleName(), currentAuth.getAuthorities());
        } else {
            log.warn("⚠️ [JwtAuthFilter] SecurityContext пуст после обработки!");
        }

        log.info("🔍 [JwtAuthFilter] Передаем запрос дальше по цепочке фильтров");
        filterChain.doFilter(request, response);
    }

    private boolean jwtValid(String jwt) {
        try {
            return jwt != null && jwtUtil.validateToken(jwt);
        } catch (Exception ex) {
            return false;
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

