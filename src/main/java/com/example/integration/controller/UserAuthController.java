package com.example.integration.controller;

import com.example.integration.dto.user.UserAuthResponse;
import com.example.integration.dto.user.UserLoginRequest;
import com.example.integration.dto.user.UserRegisterRequest;
import com.example.integration.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/user/auth")
public class UserAuthController {

    private final UserService userService;

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Value("${oauth.google.redirect-uri:https://sergiologino-ai-integration-front-cd2e.twc1.net/login/oauth2/code/google}")
    private String googleRedirectUri;

    @Value("${oauth.yandex.client-id:}")
    private String yandexClientId;

    @Value("${oauth.yandex.redirect-uri:https://sergiologino-ai-integration-front-cd2e.twc1.net/login/oauth2/code/yandex}")
    private String yandexRedirectUri;

    @Value("${oauth.frontend.base:https://sergiologino-ai-integration-front-cd2e.twc1.net}")
    private String oauthFrontendBase;

    public UserAuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterRequest request) {
        try {
            UserAuthResponse resp = userService.register(request);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        try {
            UserAuthResponse resp = userService.login(request);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
    }

    // Эндпоинт /me можно будет вернуть позже при необходимости

    @GetMapping("/oauth2/authorize/{provider}")
    public ResponseEntity<?> oauthAuthorize(@PathVariable("provider") String provider) {
        String state = userService.createOAuthState();
        String redirectUrl;
        if ("google".equalsIgnoreCase(provider)) {
            String url = "https://accounts.google.com/o/oauth2/v2/auth" +
                    "?response_type=code" +
                    "&client_id=" + urlEnc(googleClientId) +
                    "&redirect_uri=" + urlEnc(googleRedirectUri) +
                    "&scope=" + urlEnc("openid email profile") +
                    "&state=" + urlEnc(state) +
                    "&access_type=online";
            redirectUrl = url;
        } else if ("yandex".equalsIgnoreCase(provider)) {
            String url = "https://oauth.yandex.ru/authorize" +
                    "?response_type=code" +
                    "&client_id=" + urlEnc(yandexClientId) +
                    "&redirect_uri=" + urlEnc(yandexRedirectUri) +
                    "&state=" + urlEnc(state);
            redirectUrl = url;
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Неизвестный провайдер"));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // Заглушка: колбэки пока возвращают 400, пока не настроены client_id/secret и обмен кода
    @GetMapping("/oauth2/callback/{provider}")
    public ResponseEntity<?> oauthCallbackNew(@PathVariable("provider") String provider,
                                              @RequestParam(name = "code", required = false) String code,
                                              @RequestParam(name = "state", required = false) String state) {
        if (code == null || state == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "code/state не переданы"));
        }
        if (!userService.validateAndConsumeState(state)) {
            return ResponseEntity.badRequest().body(Map.of("error", "state некорректен или истёк"));
        }
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "OAuth не настроен: отсутствуют client_id/secret"));
    }

    // Допускаем совместимость с путями /login/oauth2/code/{provider}
    @GetMapping("/../../../login/oauth2/code/{provider}")
    public ResponseEntity<?> oauthCallbackCompat(@PathVariable("provider") String provider,
                                                 @RequestParam(name = "code", required = false) String code,
                                                 @RequestParam(name = "state", required = false) String state) {
        return oauthCallbackNew(provider, code, state);
    }

    private String urlEnc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}


