package com.example.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP-мост Qwen3-TTS (AltaPens docker/tts-qwen или отдельный хост).
 */
@Data
@ConfigurationProperties(prefix = "ai.tts")
public class TtsProperties {
    private boolean enabled = false;
    private String baseUrl = "http://127.0.0.1:8000";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 180_000;
}
