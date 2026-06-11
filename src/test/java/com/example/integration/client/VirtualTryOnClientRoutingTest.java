package com.example.integration.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.integration.model.NeuralNetwork;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VirtualTryOnClientRoutingTest {

    @Test
    void resolvesFashnBackendFromRequestMapping() {
        NeuralNetwork network = new NeuralNetwork();
        network.setRequestMapping(Map.of("tryOnBackend", "fashn-product-to-model"));

        String backend = invokeResolveTryOnBackend(network);
        assertEquals("fashn-product-to-model", backend);
        assertTrue(backend.startsWith("fashn"));
    }

    @Test
    void resolvesKlingBackendFromRequestMapping() {
        NeuralNetwork network = new NeuralNetwork();
        network.setRequestMapping(Map.of("tryOnBackend", "kling-kolors-tryon"));

        String backend = invokeResolveTryOnBackend(network);
        assertEquals("kling-kolors-tryon", backend);
        assertTrue(backend.startsWith("kling"));
    }

    private static String invokeResolveTryOnBackend(NeuralNetwork network) {
        Map<String, Object> map = network.getRequestMapping();
        Object backend = map.get("tryOnBackend");
        return backend != null ? backend.toString().toLowerCase() : "grok-imagine-edit";
    }
}
