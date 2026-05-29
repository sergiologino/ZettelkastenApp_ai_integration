package com.example.integration.support;

/**
 * Binary image fetched from a remote provider URL (e.g. short-lived xAI CDN link).
 */
public record DownloadedImage(
    String base64,
    String contentType,
    String sourceUrl,
    int byteLength
) {
}
