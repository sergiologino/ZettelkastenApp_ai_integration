package com.example.integration.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

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
    ),
    servers = {
        @Server(url = "http://localhost:8091", description = "Local development server"),
        @Server(url = "https://ai.example.com", description = "Production server")
    }
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
}

