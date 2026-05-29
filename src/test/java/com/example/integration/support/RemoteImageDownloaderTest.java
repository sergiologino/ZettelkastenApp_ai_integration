package com.example.integration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RemoteImageDownloaderTest {

    @Test
    void guessContentTypeFromUrlExtension() {
        assertEquals("image/png", RemoteImageDownloader.guessContentType("https://cdn.example/a.png"));
        assertEquals("image/webp", RemoteImageDownloader.guessContentType("https://cdn.example/a.webp"));
        assertEquals("image/jpeg", RemoteImageDownloader.guessContentType("https://imgen.x.ai/xai-tmp.jpeg"));
    }
}
