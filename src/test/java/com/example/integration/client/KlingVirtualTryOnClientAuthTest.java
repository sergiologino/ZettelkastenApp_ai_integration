package com.example.integration.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import com.example.integration.model.NeuralNetwork;
import org.junit.jupiter.api.Test;

class KlingVirtualTryOnClientAuthTest {

    @Test
    void generateKlingJwtUsesAccessKeyIssuerAndSecretKeySignature() {
        String accessKey = "test-access-key";
        String secretKey = "test-secret-key-with-at-least-32-bytes";
        Instant now = Instant.now();

        String token = KlingVirtualTryOnClient.generateKlingJwt(accessKey, secretKey, now);
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .build()
            .parseClaimsJws(token)
            .getBody();

        assertEquals(accessKey, claims.getIssuer());
        assertEquals(now.minusSeconds(5).getEpochSecond(), claims.getNotBefore().toInstant().getEpochSecond());
        assertEquals(now.plusSeconds(1800).getEpochSecond(), claims.getExpiration().toInstant().getEpochSecond());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void usesCurrentSingaporeVirtualTryOnEndpoint() {
        NeuralNetwork network = new NeuralNetwork();
        network.setApiUrl("https://api.klingai.com");

        assertEquals(
            "https://api-singapore.klingai.com/v1/images/kolors-virtual-try-on",
            KlingVirtualTryOnClient.resolveTryOnSubmitUrl(network)
        );
        assertEquals(
            "https://api-singapore.klingai.com/v1/videos/image2video",
            KlingVirtualTryOnClient.resolveImageToVideoSubmitUrl(network)
        );
    }

    @Test
    void stripsVersionPathFromConfiguredSingaporeEndpoint() {
        NeuralNetwork network = new NeuralNetwork();
        network.setApiUrl("https://api-singapore.klingai.com/v1/images/kolors-virtual-try-on");

        assertEquals(
            "https://api-singapore.klingai.com",
            KlingVirtualTryOnClient.resolveApiBase(network)
        );
    }
}
