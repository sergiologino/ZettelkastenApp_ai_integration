package com.example.integration.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Structured console logging for /api/ai/process (request + response bodies, base64 redacted).
 */
public final class AiTrafficLogger {

    private static final Logger log = LoggerFactory.getLogger("AI_TRAFFIC");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiTrafficLogger() {
    }

    public static void logIncoming(
        String requestId,
        String clientName,
        String userId,
        String networkName,
        String requestType,
        Map<String, Object> payload
    ) {
        log.info(
            "[AI-TRAFFIC] IN requestId={} client={} userId={} network={} type={} payload={}",
            requestId,
            clientName,
            userId,
            networkName,
            requestType,
            toJson(AiPayloadLogSupport.sanitize(payload))
        );
    }

    public static void logOutgoing(
        String requestId,
        String status,
        String networkUsed,
        int executionTimeMs,
        String errorMessage,
        Map<String, Object> responsePayload
    ) {
        Map<String, Object> sanitized = responsePayload == null ? Map.of() : AiPayloadLogSupport.sanitize(responsePayload);
        if ("success".equalsIgnoreCase(status)) {
            log.info(
                "[AI-TRAFFIC] OUT requestId={} status={} network={} ms={} response={}",
                requestId,
                status,
                networkUsed,
                executionTimeMs,
                toJson(sanitized)
            );
        } else {
            log.warn(
                "[AI-TRAFFIC] OUT requestId={} status={} network={} ms={} error={} response={}",
                requestId,
                status,
                networkUsed,
                executionTimeMs,
                errorMessage,
                toJson(sanitized)
            );
        }
    }

    public static void logTryOnRoute(
        String networkName,
        boolean personImage,
        boolean garmentImage,
        boolean grokBackend,
        boolean xaiUrl,
        boolean networkApiKeyConfigured,
        String chosenProvider
    ) {
        log.info(
            "[AI-TRAFFIC] VTON route network={} personImage={} garmentImage={} grokBackend={} xaiUrl={} networkApiKey={} provider={}",
            networkName,
            personImage,
            garmentImage,
            grokBackend,
            xaiUrl,
            networkApiKeyConfigured,
            chosenProvider
        );
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
