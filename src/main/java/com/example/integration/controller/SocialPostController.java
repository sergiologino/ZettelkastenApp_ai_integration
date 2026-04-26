package com.example.integration.controller;

import com.example.integration.dto.SocialPostRequestDTO;
import com.example.integration.dto.SocialPostResponseDTO;
import com.example.integration.model.ClientApplication;
import com.example.integration.service.SocialPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social")
@Tag(name = "Social Posting API", description = "API for publishing posts to social platforms")
@SecurityRequirement(name = "X-API-Key")
public class SocialPostController {

    private final SocialPostService socialPostService;

    public SocialPostController(SocialPostService socialPostService) {
        this.socialPostService = socialPostService;
    }

    @PostMapping("/posts")
    @Operation(
        summary = "Publish social post",
        description = "Опубликовать пост в Telegram, Facebook или X. Требуется X-API-Key; ключи платформы передаются в теле запроса и не сохраняются."
    )
    public ResponseEntity<SocialPostResponseDTO> publishPost(@Valid @RequestBody SocialPostRequestDTO request,
                                                             Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ClientApplication clientApp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SocialPostResponseDTO response = socialPostService.publish(clientApp, request);
        return ResponseEntity.ok(response);
    }
}
