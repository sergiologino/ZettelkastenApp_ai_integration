package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.security.EncryptionService;
import com.example.integration.support.DownloadedImage;
import com.example.integration.support.RemoteImageDownloader;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * xAI Grok Imagine image edit — uses person + garment reference photos (true try-on path).
 */
@Component
public class XaiImagineEditClient extends BaseNeuralClient {

    private static final Logger log = LoggerFactory.getLogger(XaiImagineEditClient.class);
    private static final String DEFAULT_EDIT_URL = "https://api.x.ai/v1/images/edits";
    private static final String DEFAULT_MODEL = "grok-imagine-image-quality";

    private final RemoteImageDownloader remoteImageDownloader;
    public XaiImagineEditClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        EncryptionService encryptionService,
        RemoteImageDownloader remoteImageDownloader
    ) {
        super(restTemplate, objectMapper, encryptionService);
        this.remoteImageDownloader = remoteImageDownloader;
    }

    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        throw new UnsupportedOperationException("Use editVirtualTryOn() for multi-image try-on");
    }

    public Map<String, Object> editImages(NeuralNetwork network, Map<String, Object> payload, String editPrompt) throws Exception {
        List<String> images = collectImageInputs(payload);
        if (images.isEmpty()) {
            throw new IllegalArgumentException("Grok image edit requires at least one input image");
        }
        if (images.size() > 3) {
            throw new IllegalArgumentException("Grok image edit supports at most 3 input images; received " + images.size());
        }

        String model = network.getModelName() != null && !network.getModelName().isBlank()
            ? network.getModelName()
            : DEFAULT_MODEL;

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", editPrompt);
        body.put("aspect_ratio", stringValue(payload.get("aspectRatio"), stringValue(extractSettings(payload).get("aspectRatio"), "3:4")));
        body.put("response_format", "url");
        body.put("n", 1);
        body.put("images", images.stream().map(XaiImagineEditClient::imageRef).toList());

        HttpHeaders headers = prepareHeaders(network);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        String endpoint = resolveEditEndpoint(network);
        log.info("Grok Imagine generic image edit: POST {} model={} images={} promptLen={}", endpoint, model, images.size(), editPrompt.length());

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            endpoint,
            Objects.requireNonNull(HttpMethod.POST),
            requestEntity,
            new ParameterizedTypeReference<>() {}
        );

        return normalizeEditResponse(editPrompt, body, response.getBody());
    }

    public Map<String, Object> editVirtualTryOn(NeuralNetwork network, Map<String, Object> payload, String editPrompt) throws Exception {
        String personImage = extractString(payload, "personImageBase64");
        String garmentImage = extractString(payload, "garmentImageBase64");
        if (personImage == null || garmentImage == null) {
            throw new IllegalArgumentException("Grok try-on requires personImageBase64 and garmentImageBase64");
        }

        String model = network.getModelName() != null && !network.getModelName().isBlank()
            ? network.getModelName()
            : DEFAULT_MODEL;

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", editPrompt);
        body.put("aspect_ratio", "3:4");
        body.put("response_format", "url");
        body.put("n", 1);
        body.put(
            "images",
            List.of(
                imageRef(personImage),
                imageRef(garmentImage)
            )
        );

        HttpHeaders headers = prepareHeaders(network);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String endpoint = resolveEditEndpoint(network);
        log.info("Grok Imagine try-on: POST {} model={} promptLen={}", endpoint, model, editPrompt.length());

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            endpoint,
            Objects.requireNonNull(HttpMethod.POST),
            requestEntity,
            new ParameterizedTypeReference<>() {}
        );

        return normalizeEditResponse(editPrompt, body, response.getBody());
    }

    private String resolveEditEndpoint(NeuralNetwork network) {
        String apiUrl = network.getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            return DEFAULT_EDIT_URL;
        }
        if (apiUrl.endsWith("/images/edits")) {
            return apiUrl;
        }
        String base = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        if (base.endsWith("/v1")) {
            return base + "/images/edits";
        }
        return DEFAULT_EDIT_URL;
    }

    private static Map<String, Object> imageRef(String base64) {
        return Map.of(
            "url", toImageUrl(base64),
            "type", "image_url"
        );
    }

    private static String toImageUrl(String base64OrUrl) {
        String trimmed = base64OrUrl.trim();
        if (trimmed.startsWith("data:image/") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "data:image/jpeg;base64," + trimmed;
    }

    static List<String> collectImageInputs(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> images = new LinkedHashSet<>();
        Object declaredImages = payload.get("images");
        if (declaredImages instanceof List<?> list) {
            for (Object item : list) {
                addDeclaredImage(images, payload, item);
            }
            if (!images.isEmpty()) {
                return List.copyOf(images);
            }
        }
        for (int i = 1; i <= 10; i++) {
            addStringValue(images, payload.get("image" + i + "Base64"));
        }
        addStringValue(images, payload.get("imageBase64"));
        addStringValue(images, payload.get("sourceImageBase64"));
        addStringValue(images, payload.get("personImageBase64"));
        addStringValue(images, payload.get("garmentImageBase64"));
        return List.copyOf(images);
    }

    private static void addDeclaredImage(LinkedHashSet<String> images, Map<String, Object> payload, Object item) {
        if (item instanceof String str) {
            addStringValue(images, str);
            return;
        }
        if (!(item instanceof Map<?, ?> map)) {
            return;
        }
        addStringValue(images, map.get("base64"));
        addStringValue(images, map.get("url"));
        Object base64Field = map.get("base64Field");
        if (base64Field instanceof String field && !field.isBlank()) {
            addStringValue(images, payload.get(field));
        }
        Object field = map.get("field");
        if (field instanceof String fieldName && !fieldName.isBlank()) {
            addStringValue(images, payload.get(fieldName));
        }
    }

    private static void addStringValue(LinkedHashSet<String> images, Object value) {
        if (value instanceof String str && !str.isBlank()) {
            images.add(str.trim());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractSettings(Map<String, Object> payload) {
        Object settings = payload == null ? null : payload.get("settings");
        if (settings instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    private static String stringValue(Object value, String fallback) {
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }
        return fallback;
    }

    private Map<String, Object> normalizeEditResponse(String prompt, Map<String, Object> request, Map<String, Object> raw) {
        List<String> assets = new ArrayList<>();
        if (raw != null && raw.get("data") instanceof List<?> dataList) {
            for (Object item : dataList) {
                if (item instanceof Map<?, ?> map) {
                    Object url = map.get("url");
                    if (url instanceof String str && !str.isBlank()) {
                        assets.add(str);
                    }
                }
            }
        }

        if (assets.isEmpty()) {
            throw new IllegalStateException("Grok Imagine edit returned no image URL");
        }

        String sourceUrl = assets.get(0);
        Map<String, Object> normalized = new HashMap<>();
        normalized.put("provider", "grok-imagine-edit");
        normalized.put("prompt", prompt);
        normalized.put("request", request);
        normalized.put("rawResponse", raw);
        normalized.put("sourceImageUrl", sourceUrl);
        normalized.put("status", "success");
        normalized.put("tokensUsed", 0);

        Optional<DownloadedImage> downloaded = remoteImageDownloader.download(sourceUrl);
        if (downloaded.isPresent()) {
            DownloadedImage image = downloaded.get();
            Map<String, Object> inlined = inlinedImageEntry(image);
            normalized.put("imageBase64", image.base64());
            normalized.put("imageContentType", image.contentType());
            normalized.put("imageByteLength", image.byteLength());
            normalized.put("assets", List.of(sourceUrl));
            normalized.put("output", List.of(inlined));
            normalized.put("data", List.of(inlined));
            log.info(
                "Grok Imagine result inlined as base64 ({} bytes, {})",
                image.byteLength(),
                image.contentType()
            );
            return normalized;
        }

        log.warn("Grok Imagine result kept as remote URL (download failed): {}", sourceUrl);
        normalized.put("assets", assets);
        normalized.put("output", assets.stream().map(url -> Map.of("url", url)).toList());
        normalized.put("data", assets.stream().map(url -> Map.of("url", url)).toList());
        return normalized;
    }

    private static Map<String, Object> inlinedImageEntry(DownloadedImage image) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("base64", image.base64());
        entry.put("contentType", image.contentType());
        entry.put("sourceUrl", image.sourceUrl());
        return entry;
    }
    private static String extractString(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value instanceof String str && !str.isBlank() ? str : null;
    }
}
