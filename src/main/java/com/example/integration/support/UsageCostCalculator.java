package com.example.integration.support;

import com.example.integration.model.NeuralNetwork;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class UsageCostCalculator {

    private static final BigDecimal RUB_PER_USD = new BigDecimal("90");

    private UsageCostCalculator() {
    }

    public static BigDecimal costUsd(NeuralNetwork network, Integer tokensUsed) {
        if (network == null || tokensUsed == null || tokensUsed <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal perTokenUsd = network.getCostPerTokenUsd();
        if (perTokenUsd != null && perTokenUsd.compareTo(BigDecimal.ZERO) > 0) {
            return perTokenUsd
                .multiply(BigDecimal.valueOf(tokensUsed))
                .setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal perTokenRub = network.getCostPerTokenRub();
        if (perTokenRub != null && perTokenRub.compareTo(BigDecimal.ZERO) > 0) {
            return perTokenRub
                .multiply(BigDecimal.valueOf(tokensUsed))
                .divide(RUB_PER_USD, 6, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    }
}
