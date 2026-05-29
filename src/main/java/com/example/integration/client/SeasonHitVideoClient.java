package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.security.EncryptionService;
import com.example.integration.support.RemoteVideoDownloader;
import com.example.integration.support.XaiApiKeyResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Season hit: image-to-video via xAI Grok Imagine Video.
 * Downloads the finished clip and returns base64 to downstream apps.
 */
@Component
public class SeasonHitVideoClient extends BaseNeuralClient {

    private static final Logger log = LoggerFactory.getLogger(SeasonHitVideoClient.class);
    private static final String DEFAULT_GENERATE_URL = "https://api.x.ai/v1/videos/generations";
    private static final String DEFAULT_MODEL = "grok-imagine-video";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration MAX_WAIT = Duration.ofMinutes(3);

    private final RemoteVideoDownloader remoteVideoDownloader;
    private final XaiApiKeyResolver xaiApiKeyResolver;

    public SeasonHitVideoClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        EncryptionService encryptionService,
        RemoteVideoDownloader remoteVideoDownloader,
        XaiApiKeyResolver xaiApiKeyResolver
    ) {
        super(restTemplate, objectMapper, encryptionService);
        this.remoteVideoDownloader = remoteVideoDownloader;
        this.xaiApiKeyResolver = xaiApiKeyResolver;
    }

    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        String sourceImageBase64 = extractString(payload, "sourceImageBase64");
        if (sourceImageBase64 == null) {
            throw new IllegalArgumentException("Season hit video requires sourceImageBase64");
        }
        String prompt = extractString(payload, "prompt");
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Season hit video requires prompt");
        }

        if (xaiApiKeyResolver.resolve(network).isEmpty()) {
            throw new IllegalStateException("no_xai_api_key");
        }

        String model = network.getModelName() != null && !network.getModelName().isBlank()
            ? network.getModelName()
            : DEFAULT_MODEL;

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", prompt.trim());
        body.put("duration", extractInteger(payload, "durationSec", 6));
        body.put("aspect_ratio", extractString(payload, "aspectRatio") != null ? payload.get("aspectRatio") : "3:4");
        body.put("image", Map.of("url", toDataUri(sourceImageBase64)));

        HttpHeaders headers = prepareHeaders(network);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        String generateUrl = resolveGenerateUrl(network);

        try {
            xaiApiKeyResolver.resolve(network).ifPresent(BaseNeuralClient::setUserApiKey);
            log.info("Season hit video: POST {} model={} promptLen={}", generateUrl, model, prompt.length());

            ResponseEntity<Map<String, Object>> startResponse = restTemplate.exchange(
                generateUrl,
                Objects.requireNonNull(HttpMethod.POST),
                requestEntity,
                new ParameterizedTypeReference<>() {}
            );
            Map<String, Object> startBody = startResponse.getBody();
            if (startBody == null) {
                throw new IllegalStateException("Empty xAI video start response");
            }

            String requestId = extractString(startBody, "request_id");
            if (requestId == null) {
                requestId = extractString(startBody, "id");
            }
            if (requestId == null) {
                throw new IllegalStateException("xAI video response missing request_id");
            }

            String videoUrl = pollVideoUrl(network, requestId);
            Optional<RemoteVideoDownloader.DownloadedVideo> downloaded = remoteVideoDownloader.download(videoUrl);
            if (downloaded.isEmpty()) {
                throw new IllegalStateException("Failed to download generated video");
            }

            RemoteVideoDownloader.DownloadedVideo video = downloaded.get();
            Map<String, Object> entry = new HashMap<>();
            entry.put("base64", video.base64());
            entry.put("contentType", video.contentType());
            entry.put("sourceUrl", video.sourceUrl());

            Map<String, Object> normalized = new HashMap<>();
            normalized.put("provider", "season-hit-video");
            normalized.put("prompt", prompt);
            normalized.put("request", body);
            normalized.put("rawResponse", startBody);
            normalized.put("sourceVideoUrl", video.sourceUrl());
            normalized.put("videoBase64", video.base64());
            normalized.put("videoContentType", video.contentType());
            normalized.put("videoByteLength", video.byteLength());
            normalized.put("output", List.of(entry));
            normalized.put("data", List.of(entry));
            normalized.put("status", "success");
            normalized.put("tokensUsed", 0);
            log.info("Season hit video inlined ({} bytes, {})", video.byteLength(), video.contentType());
            return normalized;
        } finally {
            BaseNeuralClient.clearUserApiKey();
        }
    }

    private String pollVideoUrl(NeuralNetwork network, String requestId) throws InterruptedException {
        String statusUrl = resolveStatusUrl(network, requestId);
        HttpHeaders headers = prepareHeaders(network);
        long deadline = System.nanoTime() + MAX_WAIT.toNanos();

        while (System.nanoTime() < deadline) {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                statusUrl,
                Objects.requireNonNull(HttpMethod.GET),
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
            Map<String, Object> body = response.getBody();
            if (body == null) {
                Thread.sleep(POLL_INTERVAL.toMillis());
                continue;
            }
            String status = stringValue(body.get("status"));
            if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                throw new IllegalStateException("xAI video generation failed: " + body);
            }
            if ("done".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status)) {
                String url = extractVideoUrl(body);
                if (url != null) {
                    return url;
                }
            }
            Object video = body.get("video");
            if (video instanceof Map<?, ?> videoMap) {
                String url = stringValue(videoMap.get("url"));
                if (url != null && !url.isBlank()) {
                    return url;
                }
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new IllegalStateException("xAI video generation timed out");
    }

    private static String extractVideoUrl(Map<String, Object> body) {
        Object video = body.get("video");
        if (video instanceof Map<?, ?> map) {
            String url = stringValue(map.get("url"));
            if (url != null && !url.isBlank()) {
                return url;
            }
        }
        return stringValue(body.get("url"));
    }

    private String resolveGenerateUrl(NeuralNetwork network) {
        String apiUrl = network.getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            return DEFAULT_GENERATE_URL;
        }
        if (apiUrl.contains("/videos/generations")) {
            return apiUrl;
        }
        String base = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        if (base.endsWith("/v1")) {
            return base + "/videos/generations";
        }
        return DEFAULT_GENERATE_URL;
    }

    private String resolveStatusUrl(NeuralNetwork network, String requestId) {
        String apiUrl = network.getApiUrl();
        String base = "https://api.x.ai/v1";
        if (apiUrl != null && apiUrl.contains("x.ai")) {
            int idx = apiUrl.indexOf("/v1");
            if (idx >= 0) {
                base = apiUrl.substring(0, idx + 3);
            }
        }
        return base + "/videos/" + requestId;
    }

    private static String toDataUri(String base64) {
        String trimmed = base64.trim();
        if (trimmed.startsWith("data:image/")) {
            return trimmed;
        }
        return "data:image/jpeg;base64," + trimmed;
    }

    private static String extractString(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value instanceof String str && !str.isBlank() ? str : null;
    }

    private static int extractInteger(Map<String, Object> payload, String key, int fallback) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static String stringValue(Object value) {
        return value instanceof String str && !str.isBlank() ? str : null;
    }
}
