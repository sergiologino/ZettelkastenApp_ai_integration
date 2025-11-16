package com.example.integration.security;

import com.example.integration.repository.AdminUserRepository;
import com.example.integration.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

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

        String jwt = getJwtFromRequest(request);

        if (!StringUtils.hasText(jwt) || !jwtValid(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        String subject = jwtUtil.getUsernameFromToken(jwt);

        // Try admin (by username)
        var adminOpt = adminUserRepository.findByUsername(subject);
        if (adminOpt.isPresent() && Boolean.TRUE.equals(adminOpt.get().getIsActive())) {
            var auth = new UsernamePasswordAuthenticationToken(
                    adminOpt.get(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            // Fallback to end-user by email
            userAccountRepository.findByEmail(subject).ifPresent(user -> {
                if (user.isActive()) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        }

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

