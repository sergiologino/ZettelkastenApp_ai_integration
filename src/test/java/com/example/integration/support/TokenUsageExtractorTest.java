package com.example.integration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TokenUsageExtractorTest {

    @Test
    void extractsDirectTokensUsedField() {
        assertEquals(42, TokenUsageExtractor.extract(Map.of("tokensUsed", 42)));
    }

    @Test
    void extractsCreditsAsTokens() {
        assertEquals(3, TokenUsageExtractor.extract(Map.of("creditsUsed", 3)));
    }

    @Test
    void extractsOpenAiUsageBlock() {
        assertEquals(
            120,
            TokenUsageExtractor.extract(Map.of("usage", Map.of("total_tokens", 120)))
        );
    }

    @Test
    void returnsZeroWhenMissing() {
        assertEquals(0, TokenUsageExtractor.extract(Map.of()));
    }
}
