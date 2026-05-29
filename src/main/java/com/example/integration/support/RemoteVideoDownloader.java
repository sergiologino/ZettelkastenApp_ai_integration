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

@Component
public class RemoteVideoDownloader {

    private static final Logger log = LoggerFactory.getLogger(RemoteVideoDownloader.class);
    private static final int MAX_BYTES = 50 * 1024 * 1024;

    private final RestTemplate restTemplate;

    public RemoteVideoDownloader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<DownloadedVideo> download(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String trimmed = url.trim();
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
                return Optional.empty();
            }
            if (body.length > MAX_BYTES) {
                log.warn("Remote video too large ({} bytes): {}", body.length, trimmed);
                return Optional.empty();
            }
            MediaType contentType = response.getHeaders().getContentType();
            String mime = contentType != null ? contentType.toString() : guessContentType(trimmed);
            return Optional.of(new DownloadedVideo(
                Base64.getEncoder().encodeToString(body),
                mime,
                trimmed,
                body.length
            ));
        } catch (RestClientException ex) {
            log.warn("Failed to download remote video {}: {}", trimmed, ex.getMessage());
            return Optional.empty();
        }
    }

    static String guessContentType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".webm")) {
            return "video/webm";
        }
        return "video/mp4";
    }

    public record DownloadedVideo(String base64, String contentType, String sourceUrl, int byteLength) {
    }
}
