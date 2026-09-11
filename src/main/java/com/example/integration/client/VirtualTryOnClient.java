package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.security.EncryptionService;
import com.example.integration.support.AiTrafficLogger;
import com.example.integration.support.XaiApiKeyResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Virtual try-on: Grok Imagine multi-image edit or an explicitly configured dedicated try-on backend.
 */
@Component
public class VirtualTryOnClient extends BaseNeuralClient {

    private static final Logger log = LoggerFactory.getLogger(VirtualTryOnClient.class);

    private final XaiImagineEditClient xaiImagineEditClient;
    private final FashnClient fashnClient;
    private final KlingVirtualTryOnClient klingVirtualTryOnClient;
    private final XaiApiKeyResolver xaiApiKeyResolver;

    public VirtualTryOnClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        EncryptionService encryptionService,
        XaiImagineEditClient xaiImagineEditClient,
        FashnClient fashnClient,
        KlingVirtualTryOnClient klingVirtualTryOnClient,
        XaiApiKeyResolver xaiApiKeyResolver
    ) {
        super(restTemplate, objectMapper, encryptionService);
        this.xaiImagineEditClient = xaiImagineEditClient;
        this.fashnClient = fashnClient;
        this.klingVirtualTryOnClient = klingVirtualTryOnClient;
        this.xaiApiKeyResolver = xaiApiKeyResolver;
    }

    @Override
    public Map<String, Object> sendRequest(NeuralNetwork network, Map<String, Object> payload) throws Exception {
        String basePrompt = extractString(payload, "prompt");
        if (basePrompt == null || basePrompt.isBlank()) {
            throw new IllegalArgumentException("Virtual try-on requires prompt");
        }

        String personImage = extractString(payload, "personImageBase64");
        String garmentImage = extractString(payload, "garmentImageBase64");
        boolean xaiKeyPresent = hasConfiguredApiKey(network);
        String keySource = xaiApiKeyResolver.describeKeySource(network);
        AiTrafficLogger.logTryOnRoute(
            network.getName(),
            personImage != null,
            garmentImage != null,
            isGrokBackend(network),
            apiUrlPointsToXai(network),
            xaiKeyPresent,
            "evaluating keySource=" + keySource
        );
        String garmentTitle = GarmentTitleSanitizer.forPrompt(extractString(payload, "garmentTitle"));
        String garmentBrand = extractString(payload, "garmentBrand");
        String garmentCategory = extractString(payload, "garmentCategory");
        String selectedSize = extractString(payload, "selectedSize");

        String fitPromptHint = extractString(payload, "fitPromptHint");
        String figureLockPrompt = extractString(payload, "figureLockPrompt");
        String clothingSize = extractString(payload, "clothingSize");
        boolean structuredPrompt = isStructuredWibestylePrompt(basePrompt);
        String enrichedPrompt = structuredPrompt
            ? basePrompt.trim()
            : buildTryOnPrompt(
                basePrompt,
                garmentBrand,
                garmentTitle,
                garmentCategory,
                selectedSize,
                personImage,
                garmentImage,
                extractInteger(payload, "heightCm"),
                extractInteger(payload, "bustCm"),
                extractInteger(payload, "waistCm"),
                extractInteger(payload, "hipsCm"),
                clothingSize,
                fitPromptHint
            );

        String tryOnBackend = resolveTryOnBackend(network);
        if (tryOnBackend.startsWith("fashn")) {
            return delegateTryOn(fashnClient, network, payload, "virtual_try_on_fashn", tryOnBackend);
        }
        if (tryOnBackend.startsWith("kling")) {
            return delegateTryOn(klingVirtualTryOnClient, network, payload, "virtual_try_on_kling", tryOnBackend);
        }

        String skipGrokReason = grokSkipReason(network, personImage, garmentImage);
        if (skipGrokReason == null) {
            AiTrafficLogger.logTryOnRoute(
                network.getName(),
                true,
                true,
                isGrokBackend(network),
                apiUrlPointsToXai(network),
                true,
                "virtual_try_on_grok keySource=" + keySource
            );
            String editPrompt = buildGrokEditPrompt(
                structuredPrompt ? enrichedPrompt : basePrompt,
                garmentBrand,
                garmentTitle,
                garmentCategory,
                selectedSize,
                extractInteger(payload, "heightCm"),
                extractInteger(payload, "bustCm"),
                extractInteger(payload, "waistCm"),
                extractInteger(payload, "hipsCm"),
                clothingSize,
                structuredPrompt ? null : figureLockPrompt,
                structuredPrompt ? null : fitPromptHint,
                false
            );
            log.info(
                "Virtual try-on via Grok Imagine edit, keySource={}, promptLen={}, structured={}, preview={}",
                keySource,
                editPrompt.length(),
                structuredPrompt,
                promptPreview(editPrompt)
            );
            try {
                xaiApiKeyResolver.resolve(network).ifPresent(BaseNeuralClient::setUserApiKey);
                Map<String, Object> generated = tryGrokEdit(network, payload, editPrompt);
                Map<String, Object> result = new HashMap<>(generated);
                result.put("provider", "virtual_try_on_grok");
                result.put("tryOnRoute", "grok_imagine");
                result.put("xaiKeySource", keySource);
                result.put("prompt", editPrompt);
                return result;
            } catch (Exception ex) {
                if (FashionRetailSafetyPrompt.isContentModerationFailure(ex)) {
                    log.warn("Grok content moderation (keySource={}), retrying with retail-safe prompt", keySource);
                    try {
                        String safePrompt = buildGrokEditPrompt(
                            structuredPrompt ? enrichedPrompt : basePrompt,
                            garmentBrand,
                            garmentTitle,
                            garmentCategory,
                            selectedSize,
                            extractInteger(payload, "heightCm"),
                            extractInteger(payload, "bustCm"),
                            extractInteger(payload, "waistCm"),
                            extractInteger(payload, "hipsCm"),
                            clothingSize,
                            structuredPrompt ? null : figureLockPrompt,
                            structuredPrompt ? null : fitPromptHint,
                            true
                        );
                        Map<String, Object> generated = tryGrokEdit(network, payload, safePrompt);
                        Map<String, Object> result = new HashMap<>(generated);
                        result.put("provider", "virtual_try_on_grok");
                        result.put("tryOnRoute", "grok_imagine");
                        result.put("tryOnRouteReason", "grok_imagine_retail_safe_retry");
                        result.put("xaiKeySource", keySource);
                        result.put("prompt", safePrompt);
                        return result;
                    } catch (Exception retryEx) {
                        log.warn("Grok retail-safe retry also failed: {}", retryEx.getMessage());
                        throw new IllegalStateException(
                            "VTON_CONTENT_MODERATION: xAI rejected catalog try-on. "
                                + "This is retail sleepwear/homewear fitting, not adult content. "
                                + retryEx.getMessage(),
                            retryEx
                        );
                    }
                }
                throw new IllegalStateException("Grok Imagine try-on failed: " + ex.getMessage(), ex);
            } finally {
                BaseNeuralClient.clearUserApiKey();
            }
        } else if (personImage != null && garmentImage != null) {
            log.warn(
                "Grok skipped for network {}: {}.",
                network.getName(),
                skipGrokReason
            );
        }

        if (skipGrokReason != null && skipGrokReason.contains("VTON_CONTENT_MODERATION")) {
            throw new IllegalStateException(skipGrokReason);
        }
        throw new IllegalStateException("Virtual try-on requires Grok Imagine or dedicated FASHN/Kling backend: " + skipGrokReason);
    }

    /** null = Grok should run; otherwise human-readable skip reason. */
    private String grokSkipReason(NeuralNetwork network, String personImage, String garmentImage) {
        if (personImage == null || garmentImage == null) {
            return "missing_person_or_garment_image";
        }
        if (!isGrokBackend(network) && !apiUrlPointsToXai(network)) {
            return "network_not_configured_for_grok (apply Flyway V020, api_url must contain x.ai)";
        }
        if (!hasConfiguredApiKey(network)) {
            return "no_xai_api_key (set API Key on network wibestyle-vton in admin OR env XAI_API_KEY)";
        }
        return null;
    }

    private boolean hasConfiguredApiKey(NeuralNetwork network) {
        return xaiApiKeyResolver.resolve(network).isPresent();
    }

    private Map<String, Object> delegateTryOn(
        BaseNeuralClient client,
        NeuralNetwork network,
        Map<String, Object> payload,
        String provider,
        String tryOnBackend
    ) throws Exception {
        AiTrafficLogger.logTryOnRoute(
            network.getName(),
            payload.get("personImageBase64") != null,
            payload.get("garmentImageBase64") != null,
            false,
            false,
            true,
            provider + " backend=" + tryOnBackend
        );
        Map<String, Object> generated = client.sendRequest(network, payload);
        Map<String, Object> result = new HashMap<>(generated);
        result.putIfAbsent("provider", provider);
        result.putIfAbsent("tryOnRoute", tryOnBackend);
        return result;
    }

    private static String resolveTryOnBackend(NeuralNetwork network) {
        Map<String, Object> map = network.getRequestMapping();
        if (map == null || map.isEmpty()) {
            return "grok-imagine-edit";
        }
        Object backend = map.get("tryOnBackend");
        return backend != null ? backend.toString().toLowerCase() : "grok-imagine-edit";
    }

    private boolean isGrokBackend(NeuralNetwork network) {
        return resolveTryOnBackend(network).contains("grok");
    }

    private static boolean apiUrlPointsToXai(NeuralNetwork network) {
        String apiUrl = network.getApiUrl();
        return apiUrl != null && apiUrl.toLowerCase().contains("x.ai");
    }

    private static boolean isStructuredWibestylePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }
        return prompt.contains("ДАННЫЕ ПРИМЕРКИ")
            || prompt.contains("Виртуальная примерка")
            || prompt.contains("\"figureLock\"");
    }

    private static String promptPreview(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        String oneLine = prompt.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 120) + "…";
    }

    private Map<String, Object> tryGrokEdit(NeuralNetwork network, Map<String, Object> payload, String editPrompt) throws Exception {
        return xaiImagineEditClient.editVirtualTryOn(network, payload, editPrompt);
    }

    private static String buildGrokEditPrompt(
        String clientPrompt,
        String garmentBrand,
        String garmentTitle,
        String garmentCategory,
        String selectedSize,
        Integer heightCm,
        Integer bustCm,
        Integer waistCm,
        Integer hipsCm,
        String clothingSize,
        String figureLockPrompt,
        String fitPromptHint,
        boolean strictRetailSafe
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(FashionRetailSafetyPrompt.moderationContextRu(garmentTitle, garmentCategory, strictRetailSafe));
        builder.append(' ');
        if (clientPrompt != null && !clientPrompt.isBlank()) {
            builder.append(clientPrompt.trim());
            return builder.toString().replaceAll("\\s+", " ").trim();
        }
        if (figureLockPrompt != null && !figureLockPrompt.isBlank()) {
            builder.append(figureLockPrompt).append(' ');
        }
        builder.append(
            "Virtual try-on: dress the person in image1 with the exact clothing from image2. "
        );
        builder.append("image1 is the customer body reference — the figure in image1 is authoritative. ");
        builder.append("image2 is the product photo from the marketplace card. ");
        builder.append("Overlay the marketplace garment from image2 on the person; person must be fully dressed in that product. ");
        builder.append("Remove previous outfit from view — person wears only the retail product, appropriately covered. ");
        builder.append(
            "Preserve the same person identity, face, hair, skin tone from image1 only — "
                + "never copy or blend the face from image2 even when a catalog model is visible on the product photo. "
                + "image2 supplies garment only; ignore the model's face and body on image2. "
                + "BODY FIGURE PRIORITY: keep full bust volume, hip width and waist curve from image1 — "
                + "never slim breasts, hips or thighs to fit the garment. "
                + "Do not turn a curvy EU 50–52 body into a EU 44–46 silhouette. "
        );
        if (garmentBrand != null && !garmentBrand.isBlank()) {
            builder.append("Brand: ").append(garmentBrand).append(". ");
        }
        if (garmentTitle != null && !garmentTitle.isBlank()) {
            builder.append("Product: ").append(garmentTitle).append(". ");
        }
        if (selectedSize != null && !selectedSize.isBlank()) {
            builder.append("Marketplace label size on card: ").append(selectedSize).append(". ");
        }
        if (clothingSize != null && !clothingSize.isBlank()) {
            builder.append("Customer usual clothing size: ").append(clothingSize).append(". ");
        }
        appendAnthropometry(builder, heightCm, bustCm, waistCm, hipsCm);
        if (fitPromptHint != null && !fitPromptHint.isBlank()) {
            builder.append(' ').append(fitPromptHint).append(' ');
        }
        builder.append("Photorealistic full-body fashion photo, neutral studio background, natural standing pose, vertical 3:4.");
        if (figureLockPrompt != null && !figureLockPrompt.isBlank()) {
            builder.append(' ').append(figureLockPrompt);
        }
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    private static String buildTryOnPrompt(
        String basePrompt,
        String garmentBrand,
        String garmentTitle,
        String garmentCategory,
        String selectedSize,
        String personImage,
        String garmentImage,
        Integer heightCm,
        Integer bustCm,
        Integer waistCm,
        Integer hipsCm,
        String clothingSize,
        String fitPromptHint
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(FashionRetailSafetyPrompt.moderationContextRu(garmentTitle, garmentCategory, false));
        builder.append(' ');
        builder.append(basePrompt.trim());
        builder.append(" Photorealistic virtual try-on result.");
        if (garmentBrand != null && !garmentBrand.isBlank()) {
            builder.append(" Brand: ").append(garmentBrand).append('.');
        }
        if (garmentTitle != null && !garmentTitle.isBlank()) {
            builder.append(" Garment: ").append(garmentTitle).append('.');
        }
        if (selectedSize != null && !selectedSize.isBlank()) {
            builder.append(" Marketplace label size: ").append(selectedSize).append('.');
        }
        if (clothingSize != null && !clothingSize.isBlank()) {
            builder.append(" Customer usual size: ").append(clothingSize).append('.');
        }
        appendAnthropometry(builder, heightCm, bustCm, waistCm, hipsCm);
        if (fitPromptHint != null && !fitPromptHint.isBlank()) {
            builder.append(' ').append(fitPromptHint);
        }
        if (personImage != null && !personImage.isBlank()) {
            builder.append(
                " Keep the exact same person from the reference photo — same face, hair, skin tone, pose and body silhouette."
            );
            builder.append(
                " Preserve full bust and hip volume from the reference — do not slim the body to fit the garment."
            );
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
        builder.append(
            ". Output must match these proportions exactly — full bust and hips, not a generic slim fashion model."
        );
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

    private static String extractString(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value instanceof String str && !str.isBlank() ? str : null;
    }
}
