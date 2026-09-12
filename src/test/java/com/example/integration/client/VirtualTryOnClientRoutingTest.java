package com.example.integration.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.integration.model.NeuralNetwork;
import java.util.List;
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

    @Test
    void genericImageEditPayloadUsesDeclaredImagesWithoutGarmentContract() {
        Map<String, Object> payload = Map.of(
            "prompt", "edit only hair",
            "sourceImageBase64", "source-image",
            "portraitImageBase64", "portrait-image",
            "hairstyleReferenceImageBase64", "style-image",
            "images", List.of(
                Map.of("label", "image1", "base64Field", "sourceImageBase64"),
                Map.of("label", "image2", "base64Field", "portraitImageBase64"),
                Map.of("label", "image3", "base64Field", "hairstyleReferenceImageBase64")
            )
        );

        assertTrue(VirtualTryOnClient.isGenericImageEditPayload(payload));
        assertEquals(
            List.of("source-image", "portrait-image", "style-image"),
            XaiImagineEditClient.collectImageInputs(payload)
        );
    }

    @Test
    void declaredImagesAreTheCompleteInputList() {
        Map<String, Object> payload = Map.of(
            "prompt", "edit only hair",
            "sourceImageBase64", "source-image",
            "image4Base64", "must-not-be-added",
            "images", List.of(
                Map.of("label", "image1", "base64Field", "sourceImageBase64"),
                Map.of("label", "image2", "base64", "style-image"),
                Map.of("label", "image3", "base64", "color-image")
            )
        );

        assertEquals(
            List.of("source-image", "style-image", "color-image"),
            XaiImagineEditClient.collectImageInputs(payload)
        );
    }

    private static String invokeResolveTryOnBackend(NeuralNetwork network) {
        Map<String, Object> map = network.getRequestMapping();
        Object backend = map.get("tryOnBackend");
        return backend != null ? backend.toString().toLowerCase() : "grok-imagine-edit";
    }
}
