package com.example.integration.dto;

import jakarta.validation.constraints.Email;
import java.util.UUID;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class AssignClientUserRequest {

    private UUID userId;

    @Email(message = "Некорректный email пользователя")
    private String userEmail;

    private String userFullName;

    private boolean createUserIfMissing = false;

    public String normalizedEmail() {
        return StringUtils.hasText(userEmail) ? userEmail.trim().toLowerCase() : null;
    }
}

