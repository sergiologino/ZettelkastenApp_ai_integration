package com.example.integration.service;

import com.example.integration.model.UserAccount;
import com.example.integration.repository.UserAccountRepository;
import com.example.integration.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserContextService {

    private final JwtUtil jwtUtil;
    private final UserAccountRepository userAccountRepository;

    public UserContextService(JwtUtil jwtUtil, UserAccountRepository userAccountRepository) {
        this.jwtUtil = jwtUtil;
        this.userAccountRepository = userAccountRepository;
    }

    public Optional<UserAccount> resolveCurrentUser(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = bearerToken.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return Optional.empty();
        }
        String email = jwtUtil.getUsernameFromToken(token);
        return userAccountRepository.findByEmail(email);
    }
}


