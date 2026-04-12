package com.example.integration.tts;

import com.example.integration.config.TtsProperties;
import com.example.integration.dto.AiRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * По флагу в metadata добавляет в ответ чата поле {@code integration_tts} с WAV в base64.
 */
@Service
public class TtsEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(TtsEnrichmentService.class);

    private final TtsClient ttsClient;
    private final TtsProperties ttsProperties;

    public TtsEnrichmentService(TtsClient ttsClient, TtsProperties ttsProperties) {
        this.ttsClient = ttsClient;
        this.ttsProperties = ttsProperties;
    }

    public void enrichChatResponseIfRequested(AiRequestDTO request, Map<String, Object> neuralResponse) {
        if (!ttsProperties.isEnabled()) {
            return;
        }
        if (request == null || neuralResponse == null) {
            return;
        }
        if (!"chat".equalsIgnoreCase(request.getRequestType())) {
            return;
        }
        if (!metadataFlag(request, "synthesizeTts")) {
            return;
        }

        String assistantText = ChatAssistantTextExtractor.extractAssistantContent(neuralResponse);
        if (assistantText == null || assistantText.isBlank()) {
            log.debug("TTS skipped: no assistant text in response");
            return;
        }

        String voice = metadataValue(request, "ttsVoice");
        if (voice == null) {
            voice = "default";
        }

        ttsClient.synthesizeRussian(assistantText, voice).ifPresentOrElse(
            result -> {
                Map<String, Object> block = new LinkedHashMap<>();
                block.put("format", result.format());
                block.put("sample_rate", result.sampleRate());
                block.put("base64", result.audioBase64());
                neuralResponse.put("integration_tts", block);
            },
            () -> log.debug("TTS enrichment produced no audio (client returned empty)")
        );
    }

    private static boolean metadataFlag(AiRequestDTO request, String key) {
        String v = metadataValue(request, key);
        if (v == null) {
            return false;
        }
        String t = v.trim();
        return "true".equalsIgnoreCase(t) || "1".equals(t) || "yes".equalsIgnoreCase(t);
    }

    private static String metadataValue(AiRequestDTO request, String key) {
        if (request.getMetadata() == null) {
            return null;
        }
        return request.getMetadata().get(key);
    }
}
