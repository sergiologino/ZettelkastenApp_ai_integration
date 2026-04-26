package com.example.integration.service;

import com.example.integration.dto.SocialPostRequestDTO;
import com.example.integration.dto.SocialPostResponseDTO;
import com.example.integration.dto.SocialPostStatsDTO;
import com.example.integration.model.ClientApplication;
import com.example.integration.model.ExternalUser;
import com.example.integration.model.RequestLog;
import com.example.integration.repository.ExternalUserRepository;
import com.example.integration.repository.RequestLogRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SocialPostService {

    private static final String REQUEST_TYPE_PREFIX = "social_post:";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE =
        new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final ExternalUserRepository externalUserRepository;
    private final RequestLogRepository requestLogRepository;

    public SocialPostService(RestTemplate restTemplate,
                             ExternalUserRepository externalUserRepository,
                             RequestLogRepository requestLogRepository) {
        this.restTemplate = restTemplate;
        this.externalUserRepository = externalUserRepository;
        this.requestLogRepository = requestLogRepository;
    }

    @Transactional
    public SocialPostResponseDTO publish(ClientApplication clientApp, SocialPostRequestDTO request) {
        long startTime = System.currentTimeMillis();
        String platform = normalizePlatform(request.getPlatform());
        ExternalUser user = getOrCreateUser(clientApp, request.getUserId());
        RequestLog requestLog = createRequestLog(clientApp, user, request, platform);

        try {
            Map<String, Object> providerResponse = switch (platform) {
                case "telegram" -> publishTelegram(request);
                case "facebook" -> publishFacebook(request);
                case "x" -> publishX(request);
                default -> throw new IllegalArgumentException("Unsupported social platform: " + request.getPlatform());
            };

            int executionTime = elapsedMs(startTime);
            requestLog.markCompleted("success", providerResponse, executionTime, null);
            requestLogRepository.save(requestLog);

            SocialPostResponseDTO response = baseResponse(requestLog, platform, executionTime);
            response.setStatus("success");
            response.setProviderPostId(extractProviderPostId(platform, providerResponse));
            response.setResponse(providerResponse);
            return response;
        } catch (Exception e) {
            int executionTime = elapsedMs(startTime);
            requestLog.markFailed(providerError(e), executionTime);
            requestLogRepository.save(requestLog);

            SocialPostResponseDTO response = baseResponse(requestLog, platform, executionTime);
            response.setStatus("failed");
            response.setErrorMessage(providerError(e));
            return response;
        }
    }

    @Transactional(readOnly = true)
    public SocialPostStatsDTO getStats() {
        List<RequestLog> socialLogs = requestLogRepository.findAll().stream()
            .filter(log -> log.getRequestType() != null && log.getRequestType().startsWith(REQUEST_TYPE_PREFIX))
            .toList();

        Map<String, Long> postsByPlatform = countByPlatform(socialLogs);
        Map<String, Long> successfulByPlatform = countByPlatformWithStatus(socialLogs, "success");
        Map<String, Long> failedByPlatform = countByPlatformWithStatus(socialLogs, "failed");

        long successful = socialLogs.stream().filter(log -> "success".equals(log.getStatus())).count();
        long failed = socialLogs.stream().filter(log -> "failed".equals(log.getStatus())).count();

        return new SocialPostStatsDTO(
            socialLogs.size(),
            successful,
            failed,
            postsByPlatform,
            successfulByPlatform,
            failedByPlatform
        );
    }

    private Map<String, Object> publishTelegram(SocialPostRequestDTO request) {
        String botToken = requiredCredential(request, "botToken");
        String chatId = requiredCredential(request, "chatId");

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", request.getText());
        putOption(body, request, "parseMode", "parse_mode");
        putOption(body, request, "disableWebPagePreview", "disable_web_page_preview");

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        return exchangeJson(url, HttpMethod.POST, body, new HttpHeaders());
    }

    private Map<String, Object> publishFacebook(SocialPostRequestDTO request) {
        String accessToken = requiredCredential(request, "accessToken");
        String pageId = requiredCredential(request, "pageId");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("message", request.getText());
        body.add("access_token", accessToken);
        Object link = option(request, "link");
        if (link != null) {
            body.add("link", link.toString());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String url = "https://graph.facebook.com/v19.0/" + pageId + "/feed";
        return exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers));
    }

    private Map<String, Object> publishX(SocialPostRequestDTO request) {
        String bearerToken = requiredCredential(request, "bearerToken");

        Map<String, Object> body = new HashMap<>();
        body.put("text", request.getText());
        Object replyToTweetId = option(request, "replyToTweetId");
        if (replyToTweetId != null) {
            body.put("reply", Map.of("in_reply_to_tweet_id", replyToTweetId.toString()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        return exchangeJson("https://api.twitter.com/2/tweets", HttpMethod.POST, body, headers);
    }

    private Map<String, Object> exchangeJson(String url, HttpMethod method, Map<String, Object> body, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        return exchange(url, method, new HttpEntity<>(body, headers));
    }

    private Map<String, Object> exchange(String url, HttpMethod method, HttpEntity<?> entity) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, method, entity, MAP_RESPONSE);
        return Objects.requireNonNullElseGet(response.getBody(), Map::of);
    }

    private RequestLog createRequestLog(ClientApplication clientApp,
                                        ExternalUser user,
                                        SocialPostRequestDTO request,
                                        String platform) {
        RequestLog requestLog = new RequestLog();
        requestLog.setClientApp(clientApp);
        requestLog.setExternalUser(user);
        requestLog.setRequestType(REQUEST_TYPE_PREFIX + platform);
        requestLog.setRequestPayload(sanitizedPayload(request, platform));
        requestLog.setStatus("pending");
        return requestLogRepository.save(requestLog);
    }

    private Map<String, Object> sanitizedPayload(SocialPostRequestDTO request, String platform) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("platform", platform);
        payload.put("text", request.getText());
        payload.put("options", request.getOptions() != null ? request.getOptions() : Map.of());
        payload.put("credentialKeys", request.getCredentials().keySet());
        return payload;
    }

    private ExternalUser getOrCreateUser(ClientApplication clientApp, String externalUserId) {
        return externalUserRepository
            .findByClientAppAndExternalUserId(clientApp, externalUserId)
            .orElseGet(() -> {
                ExternalUser newUser = new ExternalUser();
                newUser.setClientApp(clientApp);
                newUser.setExternalUserId(externalUserId);
                newUser.setUserType("free_user");
                return externalUserRepository.save(newUser);
            });
    }

    private String normalizePlatform(String platform) {
        return platform == null ? "" : platform.trim().toLowerCase(Locale.ROOT);
    }

    private String requiredCredential(SocialPostRequestDTO request, String key) {
        String value = request.getCredentials().get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required credential: " + key);
        }
        return value;
    }

    private Object option(SocialPostRequestDTO request, String key) {
        return request.getOptions() == null ? null : request.getOptions().get(key);
    }

    private void putOption(Map<String, Object> body, SocialPostRequestDTO request, String optionKey, String providerKey) {
        Object value = option(request, optionKey);
        if (value != null) {
            body.put(providerKey, value);
        }
    }

    private SocialPostResponseDTO baseResponse(RequestLog requestLog, String platform, int executionTime) {
        SocialPostResponseDTO response = new SocialPostResponseDTO();
        response.setRequestId(requestLog.getId() != null ? requestLog.getId().toString() : null);
        response.setPlatform(platform);
        response.setExecutionTimeMs(executionTime);
        return response;
    }

    private String extractProviderPostId(String platform, Map<String, Object> providerResponse) {
        if ("telegram".equals(platform) && providerResponse.get("result") instanceof Map<?, ?> result) {
            Object messageId = result.get("message_id");
            return messageId != null ? messageId.toString() : null;
        }
        if ("facebook".equals(platform)) {
            Object id = providerResponse.get("id");
            return id != null ? id.toString() : null;
        }
        if ("x".equals(platform) && providerResponse.get("data") instanceof Map<?, ?> data) {
            Object id = data.get("id");
            return id != null ? id.toString() : null;
        }
        return null;
    }

    private String providerError(Exception e) {
        if (e instanceof RestClientResponseException restException) {
            return "Provider returned HTTP " + restException.getStatusCode().value() + ": " + restException.getResponseBodyAsString();
        }
        return e.getMessage();
    }

    private int elapsedMs(long startTime) {
        return (int) (System.currentTimeMillis() - startTime);
    }

    private Map<String, Long> countByPlatform(List<RequestLog> logs) {
        return logs.stream()
            .collect(Collectors.groupingBy(this::platformFromLog, Collectors.counting()));
    }

    private Map<String, Long> countByPlatformWithStatus(List<RequestLog> logs, String status) {
        return logs.stream()
            .filter(log -> status.equals(log.getStatus()))
            .collect(Collectors.groupingBy(this::platformFromLog, Collectors.counting()));
    }

    private String platformFromLog(RequestLog log) {
        return log.getRequestType().substring(REQUEST_TYPE_PREFIX.length());
    }
}
