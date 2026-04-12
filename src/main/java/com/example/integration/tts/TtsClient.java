package com.example.integration.tts;

import com.example.integration.config.TtsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class TtsClient {

    private static final Logger log = LoggerFactory.getLogger(TtsClient.class);

    private final RestTemplate restTemplate;
    private final TtsProperties properties;

    public TtsClient(
        @Qualifier("ttsRestTemplate") RestTemplate restTemplate,
        TtsProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Синтез WAV (base64) через Qwen3-TTS; только русский язык на стороне сервиса.
     */
    public Optional<TtsSynthesisResult> synthesizeRussian(String text, String voicePreset) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        String url = base + "/v1/synthesize";

        Map<String, Object> body = new HashMap<>();
        body.put("text", text.trim());
        body.put("language", "Russian");
        body.put("voice", voicePreset != null && !voicePreset.isBlank() ? voicePreset : "default");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> m = resp.getBody();
            if (m == null) {
                return Optional.empty();
            }
            Object b64 = m.get("audio_base64");
            Object sr = m.get("sample_rate");
            Object fmt = m.get("format");
            if (b64 == null) {
                log.warn("TTS response missing audio_base64, keys={}", m.keySet());
                return Optional.empty();
            }
            int sampleRate = sr instanceof Number n ? n.intValue() : 24_000;
            String format = fmt != null ? fmt.toString() : "wav";
            return Optional.of(new TtsSynthesisResult(sampleRate, format, b64.toString()));
        } catch (Exception e) {
            log.warn("TTS synthesize failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
