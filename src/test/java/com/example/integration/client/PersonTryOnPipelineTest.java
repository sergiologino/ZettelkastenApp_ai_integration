package com.example.integration.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.integration.model.NeuralNetwork;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PersonTryOnPipelineTest {

    @Test
    void fashnPhotoNetworkIsNotVideoPipeline() {
        NeuralNetwork network = new NeuralNetwork();
        network.setNetworkType("image_generation");
        network.setRequestMapping(Map.of("pipeline", "person-tryon"));
        assertFalse(isVideoPipeline(network, Map.of()));
    }

    @Test
    void fashnVideoNetworkUsesVideoPipeline() {
        NeuralNetwork network = new NeuralNetwork();
        network.setNetworkType("video_generation");
        network.setRequestMapping(Map.of("pipeline", "person-tryon-video"));
        assertTrue(isVideoPipeline(network, Map.of()));
    }

    @Test
    void outputModeVideoTriggersPipeline() {
        NeuralNetwork network = new NeuralNetwork();
        network.setNetworkType("image_generation");
        assertTrue(isVideoPipeline(network, Map.of("outputMode", "video")));
    }

    private static boolean isVideoPipeline(NeuralNetwork network, Map<String, Object> payload) {
        if ("video".equalsIgnoreCase(stringValue(payload.get("outputMode")))) {
            return true;
        }
        if (Boolean.TRUE.equals(payload.get("generateVideo"))) {
            return true;
        }
        Map<String, Object> mapping = network.getRequestMapping();
        if (mapping != null) {
            Object pipeline = mapping.get("pipeline");
            if (pipeline != null && pipeline.toString().toLowerCase().contains("video")) {
                return true;
            }
        }
        return "video_generation".equalsIgnoreCase(network.getNetworkType());
    }

    private static String stringValue(Object value) {
        return value instanceof String str && !str.isBlank() ? str : null;
    }
}
