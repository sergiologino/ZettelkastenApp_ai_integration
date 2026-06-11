package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.security.EncryptionService;
import com.example.integration.support.DownloadedImage;
import com.example.integration.support.RemoteImageDownloader;
import com.example.integration.support.RemoteVideoDownloader;
import com.example.integration.support.TryOnImageSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
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
 * FASHN AI: person+garment try-on ({@code tryon-max}) and optional video ({@code image-to-video}).
 */
@Component
public class FashnClient extends BaseNeuralClient {

    private static final Logger log = LoggerFactory.getLogger(FashnClient.class);
    private static final String DEFAULT_RUN_URL = "https://api.fashn.ai/v1/run";
    private static final String DEFAULT_STATUS_URL = "https://api.fashn.ai/v1/status";
    private static final String MODEL_TRYON_MAX = "tryon-max";
    private static final String MODEL_IMAGE_TO_VIDEO = "image-to-video";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);
    private static final Duration MAX_WAIT = Duration.ofMinutes(5);

    private final RemoteImageDownloader remoteImageDownloader;
    private final RemoteVideoDownloader remoteVideoDownloader;

    public FashnClient(
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
        if (isPersonTryOnVideoPipeline(network, payload)) {
            return sendPersonTryOnVideo(network, payload);
        }
        String modelName = resolveModelName(network, payload);
        Map<String, Object> inputs = buildInputs(network, modelName, payload);
        Map<String, Object> completed = runFashnModel(network, modelName, inputs);
        return normalizeImageResponse(network, modelName, inputs, completed);
    }

    private Map<String, Object> sendPersonTryOnVideo(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        Map<String, Object> tryOnInputs = buildInputs(network, MODEL_TRYON_MAX, payload);
        Map<String, Object> tryOnCompleted = runFashnModel(network, MODEL_TRYON_MAX, tryOnInputs);
        Map<String, Object> tryOnNormalized = normalizeImageResponse(network, MODEL_TRYON_MAX, tryOnInputs, tryOnCompleted);

        String tryOnImageRef = resolveImageReference(tryOnNormalized);
        Map<String, Object> videoInputs = buildVideoInputs(network, payload, tryOnImageRef);
        Map<String, Object> videoCompleted = runFashnModel(network, MODEL_IMAGE_TO_VIDEO, videoInputs);

        int tryOnCredits = estimateCredits(network, MODEL_TRYON_MAX, tryOnInputs);
        int videoCredits = estimateVideoCredits(network, videoInputs);
        return normalizeVideoResponse(network, tryOnNormalized, videoInputs, videoCompleted, tryOnCredits + videoCredits);
    }

    private Map<String, Object> runFashnModel(NeuralNetwork network, String modelName, Map<String, Object> inputs) throws Exception {
        Map<String, Object> runBody = Map.of("model_name", modelName, "inputs", inputs);
        HttpHeaders headers = prepareHeaders(network);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(runBody, headers);
        String runUrl = resolveRunUrl(network);

        log.info("FASHN: POST {} model={}", runUrl, modelName);
        ResponseEntity<Map<String, Object>> startResponse = restTemplate.exchange(
            runUrl,
            Objects.requireNonNull(HttpMethod.POST),
            requestEntity,
            new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> startBody = startResponse.getBody();
        if (startBody == null) {
            throw new IllegalStateException("Empty FASHN start response");
        }
        Object error = startBody.get("error");
        if (error != null && !String.valueOf(error).isBlank() && !"null".equalsIgnoreCase(String.valueOf(error))) {
            throw new IllegalStateException("FASHN run error: " + error);
        }

        String predictionId = stringValue(startBody.get("id"));
        if (predictionId == null) {
            throw new IllegalStateException("FASHN response missing prediction id");
        }
        return pollStatus(network, predictionId);
    }

    private Map<String, Object> buildInputs(NeuralNetwork network, String modelName, Map<String, Object> payload) {
        String garmentImage = TryOnImageSupport.extractString(payload, "garmentImageBase64");
        if (garmentImage == null) {
            garmentImage = TryOnImageSupport.extractString(payload, "productImageBase64");
        }
        if (garmentImage == null) {
            throw new IllegalArgumentException("FASHN requires garmentImageBase64 or productImageBase64");
        }

        String personImage = TryOnImageSupport.extractString(payload, "personImageBase64");
        if (MODEL_TRYON_MAX.equalsIgnoreCase(modelName)) {
            if (personImage == null) {
                throw new IllegalArgumentException(
                    "FASHN try-on requires personImageBase64 — your person photo to dress in the garment"
                );
            }
        }

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("product_image", TryOnImageSupport.toFashnImage(garmentImage));
        inputs.put("return_base64", true);

        if (personImage != null && MODEL_TRYON_MAX.equalsIgnoreCase(modelName)) {
            inputs.put("model_image", TryOnImageSupport.toFashnImage(personImage));
        }

        String prompt = TryOnImageSupport.extractString(payload, "prompt");
        if (prompt != null) {
            inputs.put("prompt", prompt);
        }

        copyOptional(payload, inputs, "resolution", "generation_mode", "aspect_ratio", "output_format", "num_images", "seed");
        applyRequestMappingDefaults(network, inputs);
        return inputs;
    }

    private Map<String, Object> buildVideoInputs(NeuralNetwork network, Map<String, Object> payload, String imageRef) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("image", imageRef);

        Object duration = payload.get("durationSec");
        if (duration == null) {
            duration = payload.get("duration");
        }
        inputs.put("duration", duration instanceof Number number ? number.intValue() : 5);

        Object resolution = payload.get("videoResolution");
        if (resolution == null) {
            resolution = payload.get("resolution");
        }
        if (resolution != null) {
            inputs.put("resolution", resolution);
        } else {
            inputs.put("resolution", "720p");
        }

        String videoPrompt = TryOnImageSupport.extractString(payload, "videoPrompt");
        if (videoPrompt == null) {
            videoPrompt = TryOnImageSupport.extractString(payload, "motionPrompt");
        }
        if (videoPrompt != null) {
            inputs.put("prompt", videoPrompt);
        }

        String endImage = TryOnImageSupport.extractString(payload, "endImageBase64");
        if (endImage != null) {
            inputs.put("end_image", TryOnImageSupport.toFashnImage(endImage));
        }

        applyRequestMappingDefaults(network, inputs);
        return inputs;
    }

    private static void copyOptional(Map<String, Object> payload, Map<String, Object> inputs, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                inputs.put(key, value);
            }
        }
    }

    private static void applyRequestMappingDefaults(NeuralNetwork network, Map<String, Object> inputs) {
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping == null) {
            return;
        }
        Object defaults = mapping.get("fashnDefaults");
        if (defaults instanceof Map<?, ?> defaultsMap) {
            defaultsMap.forEach((k, v) -> {
                if (v != null && !inputs.containsKey(String.valueOf(k))) {
                    inputs.put(String.valueOf(k), v);
                }
            });
        }
    }

    private Map<String, Object> pollStatus(NeuralNetwork network, String predictionId) throws InterruptedException {
        String statusUrl = resolveStatusUrl(network, predictionId);
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

            Object error = body.get("error");
            if (error != null && !String.valueOf(error).isBlank() && !"null".equalsIgnoreCase(String.valueOf(error))) {
                throw new IllegalStateException("FASHN generation failed: " + error);
            }

            String status = stringValue(body.get("status"));
            if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                throw new IllegalStateException("FASHN generation failed: " + body);
            }
            if ("completed".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
                Object output = body.get("output");
                if (output instanceof List<?> list && !list.isEmpty()) {
                    return body;
                }
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new IllegalStateException("FASHN generation timed out");
    }

    private Map<String, Object> normalizeImageResponse(
        NeuralNetwork network,
        String modelName,
        Map<String, Object> inputs,
        Map<String, Object> completed
    ) {
        List<Map<String, Object>> data = new ArrayList<>();
        List<String> assets = extractOutputUrls(completed);

        for (String asset : assets) {
            data.add(inlineImageOrUrl(asset));
        }
        if (data.isEmpty()) {
            throw new IllegalStateException("FASHN completed without output images");
        }

        int creditsUsed = estimateCredits(network, modelName, inputs);
        Map<String, Object> normalized = new HashMap<>();
        normalized.put("provider", "fashn");
        normalized.put("tryOnRoute", modelName);
        normalized.put("model_name", modelName);
        normalized.put("request", Map.of("model_name", modelName, "inputs", inputs));
        normalized.put("rawResponse", completed);
        normalized.put("status", "success");
        normalized.put("creditsUsed", creditsUsed);
        normalized.put("tokensUsed", creditsUsed);
        normalized.put("assets", assets);
        normalized.put("output", data);
        normalized.put("data", data);
        if (data.get(0).get("base64") instanceof String base64) {
            normalized.put("imageBase64", base64);
            normalized.put("sourceImageUrl", data.get(0).get("sourceUrl"));
        } else if (data.get(0).get("url") instanceof String url) {
            normalized.put("sourceImageUrl", url);
        }
        return normalized;
    }

    private Map<String, Object> normalizeVideoResponse(
        NeuralNetwork network,
        Map<String, Object> tryOnNormalized,
        Map<String, Object> videoInputs,
        Map<String, Object> videoCompleted,
        int totalCredits
    ) {
        List<String> videoUrls = extractOutputUrls(videoCompleted);
        if (videoUrls.isEmpty()) {
            throw new IllegalStateException("FASHN video completed without output");
        }

        String videoUrl = videoUrls.get(0);
        Map<String, Object> normalized = new HashMap<>(tryOnNormalized);
        normalized.put("provider", "fashn");
        normalized.put("tryOnRoute", "tryon-max+image-to-video");
        normalized.put("model_name", MODEL_IMAGE_TO_VIDEO);
        normalized.put("videoRequest", Map.of("model_name", MODEL_IMAGE_TO_VIDEO, "inputs", videoInputs));
        normalized.put("videoRawResponse", videoCompleted);
        normalized.put("sourceVideoUrl", videoUrl);
        normalized.put("creditsUsed", totalCredits);
        normalized.put("tokensUsed", totalCredits);

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

    private static String resolveImageReference(Map<String, Object> tryOnNormalized) {
        if (tryOnNormalized.get("sourceImageUrl") instanceof String url && url.startsWith("http")) {
            return url;
        }
        if (tryOnNormalized.get("imageBase64") instanceof String base64 && !base64.isBlank()) {
            return TryOnImageSupport.toDataUri(base64, "image/png");
        }
        Object output = tryOnNormalized.get("output");
        if (output instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> map) {
            if (map.get("url") instanceof String url && !url.isBlank()) {
                return url;
            }
            if (map.get("base64") instanceof String base64 && !base64.isBlank()) {
                return TryOnImageSupport.toDataUri(base64, "image/png");
            }
        }
        throw new IllegalStateException("FASHN try-on produced no image for video step");
    }

    private static List<String> extractOutputUrls(Map<String, Object> completed) {
        List<String> urls = new ArrayList<>();
        Object output = completed.get("output");
        if (output instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String str && !str.isBlank()) {
                    urls.add(str);
                }
            }
        }
        return urls;
    }

    private Map<String, Object> inlineImageOrUrl(String imageRef) {
        Map<String, Object> entry = new HashMap<>();
        if (imageRef.startsWith("data:image/")) {
            int comma = imageRef.indexOf(',');
            String base64 = comma > 0 ? imageRef.substring(comma + 1) : imageRef;
            entry.put("base64", base64);
            entry.put("sourceUrl", imageRef);
            entry.put("contentType", "image/png");
            return entry;
        }

        Optional<DownloadedImage> downloaded = remoteImageDownloader.download(imageRef);
        if (downloaded.isPresent()) {
            DownloadedImage image = downloaded.get();
            entry.put("base64", image.base64());
            entry.put("contentType", image.contentType());
            entry.put("sourceUrl", image.sourceUrl());
            entry.put("url", image.sourceUrl());
            return entry;
        }

        entry.put("url", imageRef);
        entry.put("sourceUrl", imageRef);
        return entry;
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

    private static String resolveModelName(NeuralNetwork network, Map<String, Object> payload) {
        String personImage = TryOnImageSupport.extractString(payload, "personImageBase64");
        if (personImage != null) {
            return MODEL_TRYON_MAX;
        }
        if (network.getModelName() != null && !network.getModelName().isBlank()) {
            return network.getModelName().trim();
        }
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping != null && mapping.get("fashnModel") instanceof String model) {
            return model;
        }
        return "product-to-model";
    }

    private static int estimateCredits(NeuralNetwork network, String modelName, Map<String, Object> inputs) {
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping != null && mapping.get("creditsPerRequest") instanceof Number number && !MODEL_IMAGE_TO_VIDEO.equals(modelName)) {
            return number.intValue();
        }
        int numImages = inputs.get("num_images") instanceof Number n ? n.intValue() : 1;
        String resolution = stringValue(inputs.get("resolution"));
        String mode = stringValue(inputs.get("generation_mode"));
        int base = MODEL_TRYON_MAX.equalsIgnoreCase(modelName) ? 2 : 1;
        if ("2k".equalsIgnoreCase(resolution)) {
            base += 1;
        } else if ("4k".equalsIgnoreCase(resolution)) {
            base += 2;
        }
        if ("quality".equalsIgnoreCase(mode)) {
            base += 1;
        }
        return Math.max(1, base * Math.max(1, numImages));
    }

    private static int estimateVideoCredits(NeuralNetwork network, Map<String, Object> inputs) {
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping != null && mapping.get("videoCreditsPerRequest") instanceof Number number) {
            return number.intValue();
        }
        int duration = inputs.get("duration") instanceof Number n ? n.intValue() : 5;
        String resolution = stringValue(inputs.get("resolution"));
        if ("1080p".equalsIgnoreCase(resolution)) {
            return duration >= 10 ? 12 : 6;
        }
        if ("720p".equalsIgnoreCase(resolution)) {
            return duration >= 10 ? 6 : 3;
        }
        return duration >= 10 ? 2 : 1;
    }

    private static String resolveRunUrl(NeuralNetwork network) {
        String apiUrl = network.getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            return DEFAULT_RUN_URL;
        }
        if (apiUrl.contains("/v1/run")) {
            return apiUrl;
        }
        String base = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        if (base.endsWith("/v1")) {
            return base + "/run";
        }
        return DEFAULT_RUN_URL;
    }

    private static String resolveStatusUrl(NeuralNetwork network, String predictionId) {
        String apiUrl = network.getApiUrl();
        String base = DEFAULT_STATUS_URL;
        if (apiUrl != null && apiUrl.contains("fashn.ai")) {
            int idx = apiUrl.indexOf("/v1");
            if (idx >= 0) {
                base = apiUrl.substring(0, idx + 3) + "/status";
            }
        }
        return base + "/" + predictionId;
    }

    private static String stringValue(Object value) {
        return value instanceof String str && !str.isBlank() ? str : null;
    }
}
