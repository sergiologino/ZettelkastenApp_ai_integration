package com.example.integration.controller;

import com.example.integration.model.UserAccount;
import com.example.integration.security.JwtUtil;
import com.example.integration.service.OAuthService;
import com.example.integration.service.OAuthService.GoogleUserInfo;
import com.example.integration.service.OAuthService.YandexUserInfo;
import com.example.integration.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class OAuthCallbackController {

    private static final Logger log = LoggerFactory.getLogger(OAuthCallbackController.class);
    
    private final UserService userService;
    private final OAuthService oAuthService;
    private final JwtUtil jwtUtil;

    @Value("${oauth.frontend.base:https://sergiologino-ai-integration-front-cd2e.twc1.net}")
    private String oauthFrontendBase;

    public OAuthCallbackController(UserService userService, OAuthService oAuthService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.oAuthService = oAuthService;
        this.jwtUtil = jwtUtil;
    }

    // Обработчик для пути /login/oauth2/code/{provider} (используется Google/Yandex)
    @GetMapping("/login/oauth2/code/{provider}")
    public ResponseEntity<?> oauthCallback(@PathVariable("provider") String provider,
                                            @RequestParam(name = "code", required = false) String code,
                                            @RequestParam(name = "state", required = false) String state) {
        log.info("🔐 OAuth callback получен: provider={}, code={}, state={}", provider, code != null ? "present" : "null", state != null ? "present" : "null");
        if (code == null || state == null) {
            String redirectTo = oauthFrontendBase + "/auth?ok=0&error=" + urlEnc("code/state не переданы");
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectTo));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
        if (!userService.validateAndConsumeState(state)) {
            String redirectTo = oauthFrontendBase + "/auth?ok=0&error=" + urlEnc("state некорректен или истёк");
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectTo));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
        try {
            String token;
            if ("google".equalsIgnoreCase(provider)) {
                GoogleUserInfo info = oAuthService.exchangeGoogle(code);
                UserAccount user = userService.upsertOauthUser("google", info.sub, info.email, info.name);
                token = jwtUtil.generateToken(user.getEmail());
            } else if ("yandex".equalsIgnoreCase(provider)) {
                YandexUserInfo info = oAuthService.exchangeYandex(code);
                UserAccount user = userService.upsertOauthUser("yandex", info.id, info.defaultEmail, info.displayName);
                token = jwtUtil.generateToken(user.getEmail());
            } else {
                String redirectTo = oauthFrontendBase + "/auth?ok=0&error=" + urlEnc("Неизвестный провайдер");
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(URI.create(redirectTo));
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }
            String redirectTo = oauthFrontendBase
                    + "/auth?ok=1&provider=" + urlEnc(provider)
                    + "&token=" + urlEnc(token);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectTo));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception ex) {
            String redirectTo = oauthFrontendBase
                    + "/auth?ok=0&provider=" + urlEnc(provider)
                    + "&error=" + urlEnc(ex.getMessage() == null ? "oauth_failed" : ex.getMessage());
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectTo));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    private String urlEnc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}

