package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.security.EncryptionService;
import com.example.integration.support.DownloadedImage;
import com.example.integration.support.RemoteImageDownloader;
import com.example.integration.support.RemoteVideoDownloader;
import com.example.integration.support.TryOnImageSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
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
import javax.crypto.spec.SecretKeySpec;

/**
 * Kling AI: person+garment try-on ({@code kolors-virtual-try-on}) and optional video ({@code image2video}).
 */
@Component
public class KlingVirtualTryOnClient extends BaseNeuralClient {

    private static final Logger log = LoggerFactory.getLogger(KlingVirtualTryOnClient.class);
    private static final String DEFAULT_API_BASE = "https://api-singapore.klingai.com";
    private static final String LEGACY_API_BASE = "https://api.klingai.com";
    private static final String TRYON_PATH = "/v1/images/kolors-virtual-try-on";
    private static final String IMAGE2VIDEO_PATH = "/v1/videos/image2video";
    private static final String DEFAULT_TRYON_MODEL = "kolors-virtual-try-on-v1-5";
    private static final String DEFAULT_VIDEO_MODEL = "kling-v1-6";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);
    private static final Duration MAX_WAIT = Duration.ofMinutes(5);
    private static final Duration JWT_TTL = Duration.ofMinutes(30);

    private final RemoteImageDownloader remoteImageDownloader;
    private final RemoteVideoDownloader remoteVideoDownloader;

    public KlingVirtualTryOnClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        EncryptionService encryptionService,
        RemoteImageDownloader remoteImageDownloader,
        RemoteVideoDownloader remoteVideoDownloader
    ) {
        super(restTemplate, objectMapper, encryptionService);
        this.remoteImageDownloader = remoteImageDownloader;
        this.remoteVideoDownloader = remoteVideoDownloader;
    }

    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        validatePersonGarmentPayload(payload);
        Map<String, Object> tryOnResult = runTryOn(network, payload);
        if (isPersonTryOnVideoPipeline(network, payload)) {
            return runTryOnVideo(network, payload, tryOnResult);
        }
        return tryOnResult;
    }

    private Map<String, Object> runTryOn(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        String personImage = TryOnImageSupport.extractString(payload, "personImageBase64");
        String garmentImage = TryOnImageSupport.extractString(payload, "garmentImageBase64");

        String modelName = network.getModelName() != null && !network.getModelName().isBlank()
            && !network.getModelName().startsWith("kling-v")
            ? network.getModelName()
            : DEFAULT_TRYON_MODEL;

        Map<String, Object> body = new HashMap<>();
        body.put("model_name", modelName);
        body.put("human_image", TryOnImageSupport.toKlingImage(personImage));
        body.put("cloth_image", TryOnImageSupport.toKlingImage(garmentImage));

        String callbackUrl = TryOnImageSupport.extractString(payload, "callbackUrl");
        if (callbackUrl != null) {
            body.put("callback_url", callbackUrl);
        }

        HttpHeaders headers = prepareKlingHeaders(network);
        String submitUrl = resolveTryOnSubmitUrl(network);
        log.info("Kling try-on: POST {} model={}", submitUrl, modelName);

        ResponseEntity<Map<String, Object>> startResponse = restTemplate.exchange(
            submitUrl,
            Objects.requireNonNull(HttpMethod.POST),
            new HttpEntity<>(body, headers),
            new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> startBody = startResponse.getBody();
        if (startBody == null) {
            throw new IllegalStateException("Empty Kling try-on start response");
        }
        ensureKlingSuccess(startBody);

        String taskId = extractTaskId(startBody);
        if (taskId == null) {
            throw new IllegalStateException("Kling try-on response missing task_id");
        }

        Map<String, Object> completed = pollTryOnTask(network, taskId);
        return normalizeTryOnResponse(network, modelName, body, completed);
    }

    private Map<String, Object> runTryOnVideo(
        NeuralNetwork network,
        Map<String, Object> payload,
        Map<String, Object> tryOnResult
    ) throws Exception {
        String imageInput = resolveTryOnImageForVideo(tryOnResult);
        String videoModel = resolveVideoModel(network);

        Map<String, Object> body = new HashMap<>();
        body.put("model_name", videoModel);
        body.put("image", imageInput);

        Object duration = payload.get("durationSec");
        if (duration == null) {
            duration = payload.get("duration");
        }
        body.put("duration", duration instanceof Number number ? String.valueOf(number.intValue()) : "5");

        String prompt = TryOnImageSupport.extractString(payload, "videoPrompt");
        if (prompt == null) {
            prompt = TryOnImageSupport.extractString(payload, "motionPrompt");
        }
        if (prompt == null) {
            prompt = TryOnImageSupport.extractString(payload, "prompt");
        }
        if (prompt != null) {
            body.put("prompt", prompt);
        }

        Object aspectRatio = payload.get("aspectRatio");
        if (aspectRatio != null) {
            body.put("aspect_ratio", aspectRatio);
        }

        HttpHeaders headers = prepareKlingHeaders(network);
        String submitUrl = resolveImageToVideoSubmitUrl(network);
        log.info("Kling try-on video: POST {} model={}", submitUrl, videoModel);

        ResponseEntity<Map<String, Object>> startResponse = restTemplate.exchange(
            submitUrl,
            Objects.requireNonNull(HttpMethod.POST),
            new HttpEntity<>(body, headers),
            new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> startBody = startResponse.getBody();
        if (startBody == null) {
            throw new IllegalStateException("Empty Kling image2video start response");
        }
        ensureKlingSuccess(startBody);

        String taskId = extractTaskId(startBody);
        if (taskId == null) {
            throw new IllegalStateException("Kling image2video response missing task_id");
        }

        Map<String, Object> completed = pollImage2VideoTask(network, taskId);
        return normalizeVideoResponse(network, tryOnResult, body, completed);
    }

    private Map<String, Object> pollTryOnTask(NeuralNetwork network, String taskId) throws InterruptedException {
        return pollKlingTask(network, resolveTryOnSubmitUrl(network) + "/" + taskId, true);
    }

    private Map<String, Object> pollImage2VideoTask(NeuralNetwork network, String taskId) throws InterruptedException {
        return pollKlingTask(network, resolveImageToVideoSubmitUrl(network) + "/" + taskId, false);
    }

    private Map<String, Object> pollKlingTask(NeuralNetwork network, String statusUrl, boolean expectImages) throws InterruptedException {
        HttpHeaders headers = prepareKlingHeaders(network);
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
            ensureKlingSuccess(body);

            Map<String, Object> data = extractData(body);
            String status = stringValue(data.get("task_status"));
            if ("failed".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Kling task failed: " + data.get("task_status_msg"));
            }
            if ("succeed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                if (expectImages && !extractImageUrls(data).isEmpty()) {
                    return body;
                }
                if (!expectImages && !extractVideoUrls(data).isEmpty()) {
                    return body;
                }
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new IllegalStateException("Kling task timed out");
    }

    private Map<String, Object> normalizeTryOnResponse(
        NeuralNetwork network,
        String modelName,
        Map<String, Object> request,
        Map<String, Object> completed
    ) {
        Map<String, Object> data = extractData(completed);
        List<String> urls = extractImageUrls(data);
        if (urls.isEmpty()) {
            throw new IllegalStateException("Kling try-on completed without images");
        }

        List<Map<String, Object>> output = new ArrayList<>();
        for (String url : urls) {
            output.add(inlineImageOrUrl(url));
        }

        int tokensUsed = resolveTryOnTokensUsed(network);
        Map<String, Object> normalized = new HashMap<>();
        normalized.put("provider", "kling");
        normalized.put("tryOnRoute", "kolors-virtual-try-on");
        normalized.put("model_name", modelName);
        normalized.put("request", request);
        normalized.put("rawResponse", completed);
        normalized.put("status", "success");
        normalized.put("tokensUsed", tokensUsed);
        normalized.put("assets", urls);
        normalized.put("output", output);
        normalized.put("data", output);
        normalized.put("sourceImageUrl", urls.get(0));
        if (output.get(0).get("base64") instanceof String base64) {
            normalized.put("imageBase64", base64);
        }
        return normalized;
    }

    private Map<String, Object> normalizeVideoResponse(
        NeuralNetwork network,
        Map<String, Object> tryOnResult,
        Map<String, Object> videoRequest,
        Map<String, Object> completed
    ) {
        Map<String, Object> data = extractData(completed);
        List<String> videoUrls = extractVideoUrls(data);
        if (videoUrls.isEmpty()) {
            throw new IllegalStateException("Kling image2video completed without video");
        }

        String videoUrl = videoUrls.get(0);
        int tokensUsed = resolveTryOnTokensUsed(network) + resolveVideoTokensUsed(network);

        Map<String, Object> normalized = new HashMap<>(tryOnResult);
        normalized.put("provider", "kling");
        normalized.put("tryOnRoute", "kolors-virtual-try-on+image2video");
        normalized.put("videoRequest", videoRequest);
        normalized.put("videoRawResponse", completed);
        normalized.put("sourceVideoUrl", videoUrl);
        normalized.put("tokensUsed", tokensUsed);

        Optional<RemoteVideoDownloader.DownloadedVideo> downloaded = remoteVideoDownloader.download(videoUrl);
        if (downloaded.isPresent()) {
            RemoteVideoDownloader.DownloadedVideo video = downloaded.get();
            Map<String, Object> entry = new HashMap<>();
            entry.put("base64", video.base64());
            entry.put("contentType", video.contentType());
            entry.put("sourceUrl", video.sourceUrl());
            normalized.put("videoBase64", video.base64());
            normalized.put("videoContentType", video.contentType());
            normalized.put("videoByteLength", video.byteLength());
            normalized.put("output", List.of(entry));
            normalized.put("data", List.of(entry));
        } else {
            normalized.put("output", List.of(Map.of("url", videoUrl, "sourceUrl", videoUrl)));
            normalized.put("data", List.of(Map.of("url", videoUrl, "sourceUrl", videoUrl)));
        }
        return normalized;
    }

    private static String resolveTryOnImageForVideo(Map<String, Object> tryOnResult) {
        if (tryOnResult.get("sourceImageUrl") instanceof String url && url.startsWith("http")) {
            return url;
        }
        if (tryOnResult.get("imageBase64") instanceof String base64 && !base64.isBlank()) {
            return TryOnImageSupport.toKlingImage(base64);
        }
        Object output = tryOnResult.get("output");
        if (output instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> map) {
            if (map.get("url") instanceof String url && url.startsWith("http")) {
                return url;
            }
            if (map.get("base64") instanceof String base64 && !base64.isBlank()) {
                return TryOnImageSupport.toKlingImage(base64);
            }
        }
        throw new IllegalStateException("Kling try-on produced no image for video step");
    }

    private Map<String, Object> inlineImageOrUrl(String url) {
        Map<String, Object> entry = new HashMap<>();
        Optional<DownloadedImage> downloaded = remoteImageDownloader.download(url);
        if (downloaded.isPresent()) {
            DownloadedImage image = downloaded.get();
            entry.put("base64", image.base64());
            entry.put("contentType", image.contentType());
            entry.put("sourceUrl", image.sourceUrl());
            entry.put("url", image.sourceUrl());
            return entry;
        }
        entry.put("url", url);
        entry.put("sourceUrl", url);
        return entry;
    }

    private static void validatePersonGarmentPayload(Map<String, Object> payload) {
        String personImage = TryOnImageSupport.extractString(payload, "personImageBase64");
        String garmentImage = TryOnImageSupport.extractString(payload, "garmentImageBase64");
        if (personImage == null || garmentImage == null) {
            throw new IllegalArgumentException(
                "Virtual try-on requires personImageBase64 and garmentImageBase64 — your person and garment photos"
            );
        }
    }

    private static boolean isPersonTryOnVideoPipeline(NeuralNetwork network, Map<String, Object> payload) {
        if ("video".equalsIgnoreCase(stringValue(payload.get("outputMode")))) {
            return true;
        }
        if (Boolean.TRUE.equals(payload.get("generateVideo"))) {
            return true;
        }
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping != null) {
            Object pipeline = mapping.get("pipeline");
            if (pipeline != null && pipeline.toString().toLowerCase().contains("video")) {
                return true;
            }
        }
        return "video_generation".equalsIgnoreCase(network.getNetworkType());
    }

    private static String resolveVideoModel(NeuralNetwork network) {
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping != null && mapping.get("videoModel") instanceof String model && !model.isBlank()) {
            return model;
        }
        if (network.getModelName() != null && network.getModelName().startsWith("kling-v")) {
            return network.getModelName();
        }
        return DEFAULT_VIDEO_MODEL;
    }

    private static int resolveTryOnTokensUsed(NeuralNetwork network) {
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping != null && mapping.get("tryOnTokensPerRequest") instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        if (mapping != null && mapping.get("tokensPerRequest") instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        return 1;
    }

    private static int resolveVideoTokensUsed(NeuralNetwork network) {
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping != null && mapping.get("videoTokensPerRequest") instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractData(Map<String, Object> body) {
        Object data = body.get("data");
        if (data instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractImageUrls(Map<String, Object> data) {
        List<String> urls = new ArrayList<>();
        Object taskResult = data.get("task_result");
        if (taskResult instanceof Map<?, ?> resultMap) {
            Object images = resultMap.get("images");
            if (images instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> imageMap && imageMap.get("url") instanceof String url && !url.isBlank()) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractVideoUrls(Map<String, Object> data) {
        List<String> urls = new ArrayList<>();
        Object taskResult = data.get("task_result");
        if (taskResult instanceof Map<?, ?> resultMap) {
            Object videos = resultMap.get("videos");
            if (videos instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> videoMap && videoMap.get("url") instanceof String url && !url.isBlank()) {
                        urls.add(url);
                    }
                }
            }
        }
        if (urls.isEmpty() && data.get("url") instanceof String url && !url.isBlank()) {
            urls.add(url);
        }
        return urls;
    }

    private static String extractTaskId(Map<String, Object> body) {
        Map<String, Object> data = extractData(body);
        String taskId = stringValue(data.get("task_id"));
        if (taskId != null) {
            return taskId;
        }
        return stringValue(body.get("task_id"));
    }

    private static void ensureKlingSuccess(Map<String, Object> body) {
        Object code = body.get("code");
        if (code instanceof Number number && number.intValue() != 0) {
            throw new IllegalStateException("Kling API error: " + body.get("message"));
        }
    }

    private HttpHeaders prepareKlingHeaders(NeuralNetwork network) {
        String accessKey = decryptRequired(network.getApiKeyEncrypted(), "Kling Access Key is not configured");
        String secretKey = decryptRequired(network.getApiSecretEncrypted(), "Kling Secret Key is not configured");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateKlingJwt(accessKey, secretKey, Instant.now()));
        return headers;
    }

    static String generateKlingJwt(String accessKey, String secretKey, Instant now) {
        Instant notBefore = now.minusSeconds(5);
        Instant expiresAt = now.plus(JWT_TTL);
        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        return Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setIssuer(accessKey)
            .setNotBefore(Date.from(notBefore))
            .setExpiration(Date.from(expiresAt))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    private String decryptRequired(String encryptedValue, String message) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            throw new IllegalStateException(message);
        }
        String decrypted = encryptionService.decrypt(encryptedValue);
        if (decrypted == null || decrypted.isBlank()) {
            throw new IllegalStateException(message);
        }
        return decrypted;
    }

    static String resolveTryOnSubmitUrl(NeuralNetwork network) {
        return resolveApiBase(network) + TRYON_PATH;
    }

    static String resolveImageToVideoSubmitUrl(NeuralNetwork network) {
        return resolveApiBase(network) + IMAGE2VIDEO_PATH;
    }

    static String resolveApiBase(NeuralNetwork network) {
        String apiUrl = network.getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            return DEFAULT_API_BASE;
        }
        String trimmed = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        if (trimmed.equalsIgnoreCase(LEGACY_API_BASE)) {
            return DEFAULT_API_BASE;
        }
        int versionPath = trimmed.indexOf("/v1/");
        if (versionPath >= 0) {
            return trimmed.substring(0, versionPath);
        }
        int legacyVersionPath = trimmed.indexOf("/kling/v1");
        if (legacyVersionPath >= 0) {
            return trimmed.substring(0, legacyVersionPath);
        }
        return trimmed;
    }

    private static String stringValue(Object value) {
        return value instanceof String str && !str.isBlank() ? str : null;
    }
}
