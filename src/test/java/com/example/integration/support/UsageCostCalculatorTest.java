package com.example.integration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.integration.model.NeuralNetwork;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UsageCostCalculatorTest {

    @Test
    void usesUsdRateWhenConfigured() {
        NeuralNetwork network = new NeuralNetwork();
        network.setCostPerTokenUsd(new BigDecimal("0.000002"));

        assertEquals(
            new BigDecimal("0.004000"),
            UsageCostCalculator.costUsd(network, 2000)
        );
    }

    @Test
    void fallsBackToRubRate() {
        NeuralNetwork network = new NeuralNetwork();
        network.setCostPerTokenRub(new BigDecimal("0.000180"));

        assertEquals(
            new BigDecimal("0.002000"),
            UsageCostCalculator.costUsd(network, 1000)
        );
    }
}
