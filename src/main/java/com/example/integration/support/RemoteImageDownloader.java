package com.example.integration.support;

import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Downloads generated images from provider URLs while noteapp still has access
 * (e.g. xAI temporary imgen links that expire or are blocked for downstream apps).
 */
@Component
public class RemoteImageDownloader {

    private static final Logger log = LoggerFactory.getLogger(RemoteImageDownloader.class);
    private static final int MAX_BYTES = 15 * 1024 * 1024;

    private final RestTemplate restTemplate;

    public RemoteImageDownloader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<DownloadedImage> download(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("data:")) {
            return parseDataUri(trimmed);
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.ALL));
            headers.set(HttpHeaders.USER_AGENT, "noteapp-ai-integration/1.0");

            ResponseEntity<byte[]> response = restTemplate.exchange(
                trimmed,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
            );
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                log.warn("Remote image download returned empty body: {}", trimmed);
                return Optional.empty();
            }
            if (body.length > MAX_BYTES) {
                log.warn("Remote image too large ({} bytes), limit {}: {}", body.length, MAX_BYTES, trimmed);
                return Optional.empty();
            }

            MediaType contentType = response.getHeaders().getContentType();
            String mime = contentType != null && contentType.getType().equals("image")
                ? contentType.toString()
                : guessContentType(trimmed);
            String base64 = Base64.getEncoder().encodeToString(body);
            return Optional.of(new DownloadedImage(base64, mime, trimmed, body.length));
        } catch (RestClientException ex) {
            log.warn("Failed to download remote image {}: {}", trimmed, ex.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<DownloadedImage> parseDataUri(String dataUri) {
        int comma = dataUri.indexOf(',');
        if (comma <= 0 || comma >= dataUri.length() - 1) {
            return Optional.empty();
        }
        String meta = dataUri.substring(0, comma);
        String payload = dataUri.substring(comma + 1).trim();
        if (payload.isBlank()) {
            return Optional.empty();
        }
        String contentType = "image/jpeg";
        int colon = meta.indexOf(':');
        int semi = meta.indexOf(';');
        if (colon >= 0 && semi > colon) {
            contentType = meta.substring(colon + 1, semi);
        }
        return Optional.of(new DownloadedImage(payload, contentType, dataUri, payload.length()));
    }

    static String guessContentType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }
}
