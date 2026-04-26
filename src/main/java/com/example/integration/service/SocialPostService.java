package com.example.integration.service;

import com.example.integration.dto.SocialPostRequestDTO;
import com.example.integration.dto.SocialPostResponseDTO;
import com.example.integration.dto.SocialPostStatsDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.integration.model.ClientApplication;
import com.example.integration.model.ExternalUser;
import com.example.integration.model.RequestLog;
import com.example.integration.repository.ExternalUserRepository;
import com.example.integration.repository.RequestLogRepository;
import org.springframework.core.io.ByteArrayResource;
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

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        validatePostContent(request);
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

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            return publishTelegramAttachments(botToken, chatId, request);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", request.getText());
        putOption(body, request, "parseMode", "parse_mode");
        putOption(body, request, "disableWebPagePreview", "disable_web_page_preview");

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        return exchangeJson(url, HttpMethod.POST, body, new HttpHeaders());
    }

    private Map<String, Object> publishTelegramAttachments(String botToken, String chatId, SocialPostRequestDTO request) {
        List<SocialPostRequestDTO.Attachment> attachments = request.getAttachments();
        if (attachments.size() == 1) {
            return publishTelegramSingleAttachment(botToken, chatId, request, attachments.get(0));
        }

        List<Map<String, Object>> responses = new ArrayList<>();
        for (List<SocialPostRequestDTO.Attachment> group : splitTelegramMediaGroups(attachments)) {
            responses.add(publishTelegramMediaGroup(botToken, chatId, request, group, responses.isEmpty()));
        }

        return Map.of(
            "ok", responses.stream().allMatch(response -> Boolean.TRUE.equals(response.get("ok"))),
            "result", responses
        );
    }

    private Map<String, Object> publishTelegramSingleAttachment(String botToken,
                                                               String chatId,
                                                               SocialPostRequestDTO request,
                                                               SocialPostRequestDTO.Attachment attachment) {
        String telegramType = telegramMediaType(attachment);
        String endpoint = switch (telegramType) {
            case "photo" -> "sendPhoto";
            case "video" -> "sendVideo";
            case "document" -> "sendDocument";
            default -> throw new IllegalArgumentException("Unsupported Telegram attachment type: " + attachment.getType());
        };
        String providerField = telegramType;
        String url = "https://api.telegram.org/bot" + botToken + "/" + endpoint;

        if (attachment.getUrl() != null && !attachment.getUrl().isBlank()) {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put(providerField, attachment.getUrl());
            putCaption(body, request, attachment);
            putOption(body, request, "parseMode", "parse_mode");
            return exchangeJson(url, HttpMethod.POST, body, new HttpHeaders());
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add(providerField, attachmentResource(attachment, 0));
        addMultipartCaption(body, request, attachment);
        addMultipartOption(body, request, "parseMode", "parse_mode");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers));
    }

    private Map<String, Object> publishTelegramMediaGroup(String botToken,
                                                         String chatId,
                                                         SocialPostRequestDTO request,
                                                         List<SocialPostRequestDTO.Attachment> attachments,
                                                         boolean includeTextCaption) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        List<Map<String, Object>> media = new ArrayList<>();

        for (int i = 0; i < attachments.size(); i++) {
            SocialPostRequestDTO.Attachment attachment = attachments.get(i);
            String fieldName = "file" + i;
            Map<String, Object> mediaItem = new LinkedHashMap<>();
            mediaItem.put("type", telegramMediaType(attachment));

            if (attachment.getUrl() != null && !attachment.getUrl().isBlank()) {
                mediaItem.put("media", attachment.getUrl());
            } else {
                mediaItem.put("media", "attach://" + fieldName);
                body.add(fieldName, attachmentResource(attachment, i));
            }

            String caption = captionFor(attachment, includeTextCaption && i == 0 ? request.getText() : null);
            if (caption != null) {
                mediaItem.put("caption", caption);
                Object parseMode = option(request, "parseMode");
                if (parseMode != null) {
                    mediaItem.put("parse_mode", parseMode.toString());
                }
            }

            media.add(mediaItem);
        }

        body.add("media", toJson(media));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        String url = "https://api.telegram.org/bot" + botToken + "/sendMediaGroup";
        return exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers));
    }

    private Map<String, Object> publishFacebook(SocialPostRequestDTO request) {
        if (hasAttachments(request)) {
            throw new IllegalArgumentException("Facebook attachments are not supported yet by this endpoint");
        }

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
        if (hasAttachments(request)) {
            throw new IllegalArgumentException("X attachments are not supported yet by this endpoint");
        }

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
        payload.put("attachments", sanitizedAttachments(request));
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

    private void validatePostContent(SocialPostRequestDTO request) {
        if ((request.getText() == null || request.getText().isBlank()) && !hasAttachments(request)) {
            throw new IllegalArgumentException("Either text or attachments must be provided");
        }

        if (hasAttachments(request)) {
            for (SocialPostRequestDTO.Attachment attachment : request.getAttachments()) {
                boolean hasBase64 = attachment.getBase64() != null && !attachment.getBase64().isBlank();
                boolean hasUrl = attachment.getUrl() != null && !attachment.getUrl().isBlank();
                if (hasBase64 == hasUrl) {
                    throw new IllegalArgumentException("Each attachment must contain exactly one of base64 or url");
                }
            }
        }
    }

    private boolean hasAttachments(SocialPostRequestDTO request) {
        return request.getAttachments() != null && !request.getAttachments().isEmpty();
    }

    private String telegramMediaType(SocialPostRequestDTO.Attachment attachment) {
        String type = attachment.getType() == null ? "" : attachment.getType().trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "image", "photo" -> "photo";
            case "video" -> "video";
            case "document", "file" -> "document";
            default -> throw new IllegalArgumentException("Unsupported attachment type: " + attachment.getType());
        };
    }

    private List<List<SocialPostRequestDTO.Attachment>> splitTelegramMediaGroups(List<SocialPostRequestDTO.Attachment> attachments) {
        List<SocialPostRequestDTO.Attachment> photoVideo = new ArrayList<>();
        List<SocialPostRequestDTO.Attachment> documents = new ArrayList<>();

        for (SocialPostRequestDTO.Attachment attachment : attachments) {
            if ("document".equals(telegramMediaType(attachment))) {
                documents.add(attachment);
            } else {
                photoVideo.add(attachment);
            }
        }

        List<List<SocialPostRequestDTO.Attachment>> groups = new ArrayList<>();
        addTelegramChunks(groups, photoVideo);
        addTelegramChunks(groups, documents);
        return groups;
    }

    private void addTelegramChunks(List<List<SocialPostRequestDTO.Attachment>> groups,
                                   List<SocialPostRequestDTO.Attachment> attachments) {
        for (int start = 0; start < attachments.size(); start += 10) {
            groups.add(attachments.subList(start, Math.min(start + 10, attachments.size())));
        }
    }

    private ByteArrayResource attachmentResource(SocialPostRequestDTO.Attachment attachment, int index) {
        byte[] bytes = decodeAttachment(attachment.getBase64());
        String fileName = attachment.getFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = "attachment-" + index;
        }
        String resourceFileName = fileName;
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return resourceFileName;
            }
        };
    }

    private byte[] decodeAttachment(String base64) {
        String normalized = base64;
        int commaIndex = normalized.indexOf(',');
        if (normalized.startsWith("data:") && commaIndex >= 0) {
            normalized = normalized.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(normalized);
    }

    private void putCaption(Map<String, Object> body,
                            SocialPostRequestDTO request,
                            SocialPostRequestDTO.Attachment attachment) {
        String caption = captionFor(attachment, request.getText());
        if (caption != null) {
            body.put("caption", caption);
        }
    }

    private void addMultipartCaption(MultiValueMap<String, Object> body,
                                     SocialPostRequestDTO request,
                                     SocialPostRequestDTO.Attachment attachment) {
        String caption = captionFor(attachment, request.getText());
        if (caption != null) {
            body.add("caption", caption);
        }
    }

    private String captionFor(SocialPostRequestDTO.Attachment attachment, String fallbackText) {
        if (attachment.getCaption() != null && !attachment.getCaption().isBlank()) {
            return attachment.getCaption();
        }
        if (fallbackText != null && !fallbackText.isBlank()) {
            return fallbackText;
        }
        return null;
    }

    private void addMultipartOption(MultiValueMap<String, Object> body,
                                    SocialPostRequestDTO request,
                                    String optionKey,
                                    String providerKey) {
        Object value = option(request, optionKey);
        if (value != null) {
            body.add(providerKey, value.toString());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize provider payload", e);
        }
    }

    private List<Map<String, Object>> sanitizedAttachments(SocialPostRequestDTO request) {
        if (!hasAttachments(request)) {
            return List.of();
        }

        return request.getAttachments().stream()
            .map(attachment -> {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("type", attachment.getType());
                metadata.put("fileName", attachment.getFileName());
                metadata.put("contentType", attachment.getContentType());
                metadata.put("source", attachment.getBase64() != null && !attachment.getBase64().isBlank() ? "base64" : "url");
                metadata.put("base64Length", attachment.getBase64() != null ? attachment.getBase64().length() : null);
                metadata.put("hasCaption", attachment.getCaption() != null && !attachment.getCaption().isBlank());
                return metadata;
            })
            .toList();
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
