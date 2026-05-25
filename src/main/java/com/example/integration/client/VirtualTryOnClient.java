package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.security.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Virtual try-on: enriches prompt with person + garment context and delegates image generation to Pollinations.
 */
@Component
public class VirtualTryOnClient extends BaseNeuralClient {

    private static final Logger log = LoggerFactory.getLogger(VirtualTryOnClient.class);

    private final PollinationsClient pollinationsClient;

    public VirtualTryOnClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        EncryptionService encryptionService,
        PollinationsClient pollinationsClient
    ) {
        super(restTemplate, objectMapper, encryptionService);
        this.pollinationsClient = pollinationsClient;
    }

    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        String basePrompt = extractString(payload, "prompt");
        if (basePrompt == null || basePrompt.isBlank()) {
            throw new IllegalArgumentException("Virtual try-on requires prompt");
        }

        String personImage = extractString(payload, "personImageBase64");
        String garmentImage = extractString(payload, "garmentImageBase64");
        String garmentTitle = extractString(payload, "garmentTitle");
        String garmentBrand = extractString(payload, "garmentBrand");
        String selectedSize = extractString(payload, "selectedSize");

        String enrichedPrompt = buildTryOnPrompt(basePrompt, garmentBrand, garmentTitle, selectedSize, personImage, garmentImage);
        log.info("Virtual try-on prompt length={} hasPerson={} hasGarment={}", enrichedPrompt.length(), personImage != null, garmentImage != null);

        Map<String, Object> generationPayload = new HashMap<>();
        generationPayload.put("prompt", enrichedPrompt);
        if (payload.get("settings") instanceof Map<?, ?> settings) {
            generationPayload.put("settings", settings);
        } else {
            generationPayload.put("settings", Map.of("aspectRatio", "3:4", "width", 768, "height", 1024));
        }
        generationPayload.put(
            "negative_prompt",
            "blurry, distorted body, extra limbs, watermark, text, logo, collage, low quality, deformed face, wrong clothing fit"
        );

        NeuralNetwork imageNetwork = resolveImageNetwork(network);
        Map<String, Object> generated = pollinationsClient.sendRequest(imageNetwork, generationPayload);

        Map<String, Object> result = new HashMap<>(generated);
        result.put("provider", "virtual_try_on");
        result.put("prompt", enrichedPrompt);
        result.put("data", extractOutputAsData(generated));
        return result;
    }

    private NeuralNetwork resolveImageNetwork(NeuralNetwork network) {
        if (network.getApiUrl() != null && !network.getApiUrl().isBlank()) {
            return network;
        }
        NeuralNetwork fallback = new NeuralNetwork();
        fallback.setName(network.getName());
        fallback.setProvider("pollinations");
        fallback.setNetworkType("image_generation");
        fallback.setApiUrl("https://api.pollinations.ai/v1/images");
        fallback.setModelName(network.getModelName() != null ? network.getModelName() : "pollinations-lite");
        fallback.setRequestMapping(network.getRequestMapping());
        fallback.setResponseMapping(network.getResponseMapping());
        fallback.setApiKeyEncrypted(network.getApiKeyEncrypted());
        return fallback;
    }

    private static String buildTryOnPrompt(
        String basePrompt,
        String garmentBrand,
        String garmentTitle,
        String selectedSize,
        String personImage,
        String garmentImage
    ) {
        StringBuilder builder = new StringBuilder(basePrompt.trim());
        builder.append(" Photorealistic virtual try-on result.");
        if (garmentBrand != null && !garmentBrand.isBlank()) {
            builder.append(" Brand: ").append(garmentBrand).append('.');
        }
        if (garmentTitle != null && !garmentTitle.isBlank()) {
            builder.append(" Garment: ").append(garmentTitle).append('.');
        }
        if (selectedSize != null && !selectedSize.isBlank()) {
            builder.append(" Size fit: ").append(selectedSize).append('.');
        }
        if (personImage != null && !personImage.isBlank()) {
            builder.append(" Keep the same person body proportions, pose and skin tone from the reference person photo.");
        }
        if (garmentImage != null && !garmentImage.isBlank()) {
            builder.append(" Dress the person in the exact garment from the reference clothing photo — match color, cut, fabric and details.");
        }
        builder.append(" Full body, natural standing pose, studio lighting, clean neutral background, ecommerce fashion photography.");
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> extractOutputAsData(Map<String, Object> generated) {
        List<Map<String, String>> data = new ArrayList<>();
        Object output = generated.get("output");
        if (output instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get("url") instanceof String url && !url.isBlank()) {
                    data.add(Map.of("url", url));
                }
            }
        }
        Object assets = generated.get("assets");
        if (data.isEmpty() && assets instanceof List<?> assetList) {
            for (Object item : assetList) {
                if (item instanceof String url && !url.isBlank()) {
                    data.add(Map.of("url", url));
                }
            }
        }
        return data;
    }

    private static String extractString(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value instanceof String str && !str.isBlank() ? str : null;
    }
}
