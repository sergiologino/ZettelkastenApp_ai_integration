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
 * Virtual try-on: Grok Imagine multi-image edit when API key is configured,
 * otherwise text-only Pollinations (legacy, does not use reference photos).
 */
@Component
public class VirtualTryOnClient extends BaseNeuralClient {

    private static final Logger log = LoggerFactory.getLogger(VirtualTryOnClient.class);

    private final PollinationsClient pollinationsClient;
    private final XaiImagineEditClient xaiImagineEditClient;

    public VirtualTryOnClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        EncryptionService encryptionService,
        PollinationsClient pollinationsClient,
        XaiImagineEditClient xaiImagineEditClient
    ) {
        super(restTemplate, objectMapper, encryptionService);
        this.pollinationsClient = pollinationsClient;
        this.xaiImagineEditClient = xaiImagineEditClient;
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

        String enrichedPrompt = buildTryOnPrompt(
            basePrompt,
            garmentBrand,
            garmentTitle,
            selectedSize,
            personImage,
            garmentImage,
            extractInteger(payload, "heightCm"),
            extractInteger(payload, "bustCm"),
            extractInteger(payload, "waistCm"),
            extractInteger(payload, "hipsCm")
        );

        if (shouldUseGrokEdit(network, personImage, garmentImage)) {
            String editPrompt = buildGrokEditPrompt(
                garmentBrand,
                garmentTitle,
                selectedSize,
                extractInteger(payload, "heightCm"),
                extractInteger(payload, "bustCm"),
                extractInteger(payload, "waistCm"),
                extractInteger(payload, "hipsCm")
            );
            log.info("Virtual try-on via Grok Imagine edit, promptLen={}", editPrompt.length());
            try {
                Map<String, Object> generated = xaiImagineEditClient.editVirtualTryOn(network, payload, editPrompt);
                Map<String, Object> result = new HashMap<>(generated);
                result.put("provider", "virtual_try_on_grok");
                result.put("prompt", editPrompt);
                return result;
            } catch (Exception ex) {
                log.warn("Grok Imagine try-on failed, falling back to Pollinations text-only: {}", ex.getMessage());
            }
        } else if (personImage != null && garmentImage != null) {
            log.warn(
                "Reference photos present but Grok Imagine is not configured (set xAI API key on network {}). "
                    + "Using Pollinations text-only — garment fit will be unreliable.",
                network.getName()
            );
        }

        log.info("Virtual try-on via Pollinations text-only, promptLen={}", enrichedPrompt.length());

        Map<String, Object> generationPayload = new HashMap<>();
        generationPayload.put("prompt", enrichedPrompt);
        if (payload.get("settings") instanceof Map<?, ?> settings) {
            generationPayload.put("settings", settings);
        } else {
            generationPayload.put("settings", Map.of("aspectRatio", "3:4", "width", 768, "height", 1024));
        }
        generationPayload.put(
            "negative_prompt",
            "underwear, lingerie, white bra, white panties, wrong outfit, different person, "
                + "blurry, distorted body, extra limbs, watermark, text, logo, collage, low quality, deformed face"
        );

        NeuralNetwork imageNetwork = resolvePollinationsNetwork(network);
        Map<String, Object> generated = pollinationsClient.sendRequest(imageNetwork, generationPayload);

        Map<String, Object> result = new HashMap<>(generated);
        result.put("provider", "virtual_try_on_pollinations");
        result.put("prompt", enrichedPrompt);
        result.put("data", extractOutputAsData(generated));
        return result;
    }

    private boolean shouldUseGrokEdit(NeuralNetwork network, String personImage, String garmentImage) {
        if (personImage == null || garmentImage == null) {
            return false;
        }
        if (!isGrokBackend(network) && !apiUrlPointsToXai(network)) {
            return false;
        }
        return hasConfiguredApiKey(network);
    }

    private boolean isGrokBackend(NeuralNetwork network) {
        Map<String, Object> map = network.getRequestMapping();
        if (map == null || map.isEmpty()) {
            return false;
        }
        Object backend = map.get("tryOnBackend");
        return backend != null && backend.toString().toLowerCase().contains("grok");
    }

    private static boolean apiUrlPointsToXai(NeuralNetwork network) {
        String apiUrl = network.getApiUrl();
        return apiUrl != null && apiUrl.toLowerCase().contains("x.ai");
    }

    private boolean hasConfiguredApiKey(NeuralNetwork network) {
        if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isBlank()) {
            String decrypted = encryptionService.decrypt(network.getApiKeyEncrypted());
            if (decrypted != null && !decrypted.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String buildGrokEditPrompt(
        String garmentBrand,
        String garmentTitle,
        String selectedSize,
        Integer heightCm,
        Integer bustCm,
        Integer waistCm,
        Integer hipsCm
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(
            "Virtual try-on: dress the person in image1 with the exact clothing from image2. "
        );
        builder.append("image1 is the customer body reference. image2 is the product photo from the marketplace card. ");
        builder.append("Replace current clothes on the person with ONLY the garment from image2. ");
        builder.append("Do not leave underwear, bra, panties or the old outfit visible. ");
        builder.append("Preserve the same person identity, face, hair, skin tone and body proportions from image1. ");
        if (garmentBrand != null && !garmentBrand.isBlank()) {
            builder.append("Brand: ").append(garmentBrand).append(". ");
        }
        if (garmentTitle != null && !garmentTitle.isBlank()) {
            builder.append("Product: ").append(garmentTitle).append(". ");
        }
        if (selectedSize != null && !selectedSize.isBlank()) {
            builder.append("Size: ").append(selectedSize).append(". ");
        }
        appendAnthropometry(builder, heightCm, bustCm, waistCm, hipsCm);
        builder.append("Photorealistic full-body fashion photo, neutral studio background, natural standing pose, vertical 3:4.");
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    private NeuralNetwork resolvePollinationsNetwork(NeuralNetwork network) {
        if (network.getApiUrl() != null && network.getApiUrl().contains("pollinations")) {
            return network;
        }
        NeuralNetwork fallback = new NeuralNetwork();
        fallback.setName(network.getName());
        fallback.setProvider("pollinations");
        fallback.setNetworkType("image_generation");
        fallback.setApiUrl("https://api.pollinations.ai/v1/images");
        fallback.setModelName("pollinations-lite");
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
        String garmentImage,
        Integer heightCm,
        Integer bustCm,
        Integer waistCm,
        Integer hipsCm
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
        appendAnthropometry(builder, heightCm, bustCm, waistCm, hipsCm);
        if (personImage != null && !personImage.isBlank()) {
            builder.append(
                " Keep the exact same person from the reference photo — same face, hair, skin tone, pose and body silhouette."
            );
            builder.append(" Do not slim, widen or reshape the body.");
        }
        if (garmentImage != null && !garmentImage.isBlank()) {
            builder.append(" Dress the person in the exact garment from the reference clothing photo — match color, cut, fabric and details.");
        }
        builder.append(" Full body, natural standing pose, studio lighting, clean neutral background, vertical portrait 3:4, ecommerce fashion photography.");
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    private static void appendAnthropometry(
        StringBuilder builder,
        Integer heightCm,
        Integer bustCm,
        Integer waistCm,
        Integer hipsCm
    ) {
        if (heightCm == null && bustCm == null && waistCm == null && hipsCm == null) {
            return;
        }
        builder.append(" Exact body measurements:");
        if (heightCm != null) {
            builder.append(" height ").append(heightCm).append(" cm");
        }
        if (bustCm != null) {
            builder.append(", bust ").append(bustCm).append(" cm");
        }
        if (waistCm != null) {
            builder.append(", waist ").append(waistCm).append(" cm");
        }
        if (hipsCm != null) {
            builder.append(", hips ").append(hipsCm).append(" cm");
        }
        builder.append(". Output must match these proportions exactly — not a generic fashion model body.");
    }

    private static Integer extractInteger(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
