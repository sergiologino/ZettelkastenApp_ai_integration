package com.example.integration.security;

import com.example.integration.model.ClientApplication;
import com.example.integration.repository.ClientApplicationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    
    private final ClientApplicationRepository clientAppRepository;
    
    public ApiKeyAuthFilter(ClientApplicationRepository clientAppRepository) {
        this.clientAppRepository = clientAppRepository;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        String apiKey = request.getHeader("X-API-Key");
        
        if (apiKey != null && !apiKey.isEmpty()) {
            Optional<ClientApplication> clientApp = clientAppRepository.findByApiKey(apiKey);
            
            if (clientApp.isPresent() && clientApp.get().getIsActive()) {
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        clientApp.get(), 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"))
                    );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

