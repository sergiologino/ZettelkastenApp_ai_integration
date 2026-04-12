package com.example.integration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OAuthService {

    @Value("${oauth.google.client-id:}")
    private String googleClientId;
    @Value("${oauth.google.client-secret:}")
    private String googleClientSecret;
    @Value("${oauth.google.redirect-uri:}")
    private String googleRedirectUri;

    @Value("${oauth.yandex.client-id:}")
    private String yandexClientId;
    @Value("${oauth.yandex.client-secret:}")
    private String yandexClientSecret;
    @Value("${oauth.yandex.redirect-uri:}")
    private String yandexRedirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleUserInfo exchangeGoogle(String code) {
        // Token
        String tokenUrl = "https://oauth2.googleapis.com/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", googleRedirectUri);
        form.add("grant_type", "authorization_code");
        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);
        ResponseEntity<Map<String, Object>> resp = restTemplate.postForEntity(tokenUrl, req, (Class<Map<String, Object>>)(Class<?>)Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Не удалось получить токен Google");
        }
        String accessToken = (String) resp.getBody().get("access_token");

        // UserInfo
        HttpHeaders h2 = new HttpHeaders();
        h2.setBearerAuth(accessToken);
        ResponseEntity<Map<String, Object>> uiResp = restTemplate.exchange(
                "https://openidconnect.googleapis.com/v1/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(h2),
                (Class<Map<String, Object>>)(Class<?>)Map.class
        );
        if (!uiResp.getStatusCode().is2xxSuccessful() || uiResp.getBody() == null) {
            throw new IllegalStateException("Не удалось получить userinfo Google");
        }
        Map<String, Object> body = uiResp.getBody();
        GoogleUserInfo info = new GoogleUserInfo();
        info.sub = (String) body.get("sub");
        info.email = (String) body.get("email");
        info.name = (String) body.get("name");
        return info;
    }

    public YandexUserInfo exchangeYandex(String code) {
        // Token
        String tokenUrl = "https://oauth.yandex.ru/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", yandexClientId);
        form.add("client_secret", yandexClientSecret);
        form.add("redirect_uri", yandexRedirectUri);
        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);
        ResponseEntity<Map<String, Object>> resp = restTemplate.postForEntity(tokenUrl, req, (Class<Map<String, Object>>)(Class<?>)Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Не удалось получить токен Yandex");
        }
        String accessToken = (String) resp.getBody().get("access_token");

        // UserInfo
        HttpHeaders h2 = new HttpHeaders();
        h2.setBearerAuth(accessToken);
        ResponseEntity<Map<String, Object>> uiResp = restTemplate.exchange(
                "https://login.yandex.ru/info?format=json",
                HttpMethod.GET,
                new HttpEntity<>(h2),
                (Class<Map<String, Object>>)(Class<?>)Map.class
        );
        if (!uiResp.getStatusCode().is2xxSuccessful() || uiResp.getBody() == null) {
            throw new IllegalStateException("Не удалось получить userinfo Yandex");
        }
        Map<String, Object> body = uiResp.getBody();
        YandexUserInfo info = new YandexUserInfo();
        info.id = String.valueOf(body.get("id"));
        info.displayName = (String) body.get("real_name");
        info.defaultEmail = (String) body.get("default_email");
        return info;
    }

    public static class GoogleUserInfo {
        public String sub;
        public String email;
        public String name;
    }

    public static class YandexUserInfo {
        public String id;
        public String displayName;
        public String defaultEmail;
    }
}


