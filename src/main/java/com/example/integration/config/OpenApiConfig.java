package com.example.integration.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "AI Integration Service API",
        version = "1.0",
        description = "Universal AI integration service for multiple neural networks",
        contact = @Contact(
            name = "AI Integration Service",
            email = "support@example.com"
        )
    )
)
@SecurityScheme(
    name = "Bearer",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token for admin authentication"
)
@SecurityScheme(
    name = "X-API-Key",
    type = SecuritySchemeType.APIKEY,
    in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER,
    paramName = "X-API-Key",
    description = "API key for client application authentication"
)
public class OpenApiConfig {
    
    @Value("${swagger.server.url:}")
    private String serverUrl;
    
    @Value("${swagger.server.description:Current server}")
    private String serverDescription;
    
    /**
     * Настройка серверов для Swagger UI через переменные окружения
     * 
     * Лучшие практики:
     * 1. Если swagger.server.url не задан - используется текущий домен
     * 2. Если задан - используется указанный URL (для production)
     * 3. Локально можно не задавать - будет работать на localhost
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        
        if (serverUrl != null && !serverUrl.isEmpty()) {
            // Production: используем переменную окружения
            Server server = new Server();
            server.setUrl(serverUrl);
            server.setDescription(serverDescription);
            openAPI.servers(List.of(server));
            
            System.out.println("📝 Swagger server configured: " + serverUrl);
        } else {
            // Development: используем текущий домен (относительный URL)
            Server server = new Server();
            server.setUrl(""); // Пустая строка = текущий домен
            server.setDescription("Current server");
            openAPI.servers(List.of(server));
            
            System.out.println("📝 Swagger using current domain (relative URL)");
        }
        
        return openAPI;
    }
}

