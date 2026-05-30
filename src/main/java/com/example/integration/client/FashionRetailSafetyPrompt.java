package com.example.integration.client;

import java.util.Locale;

/**
 * Retail-safe wording for xAI content moderation on legitimate apparel try-on (sleepwear, homewear, etc.).
 */
public final class FashionRetailSafetyPrompt {

    private FashionRetailSafetyPrompt() {
    }

    public static String moderationContextRu(String garmentTitle, String garmentCategory, boolean strict) {
        StringBuilder builder = new StringBuilder();
        builder.append("КОНТЕКСТ: виртуальная примерка в интернет-магазине одежды (Wildberries/Ozon). ");
        builder.append("Покупатель примеряет товар с карточки перед покупкой — домашняя/повседневная одежда, PG, ");
        builder.append("человек полностью одет в товар с image2, нейтральная студийная поза. ");
        builder.append("НЕ эротика, НЕ порно, НЕ стриптиз, НЕ нагота. ");
        builder.append("Товар: ").append(describeGarment(garmentTitle, garmentCategory)).append(". ");
        if (strict) {
            builder.append("Консервативная каталожная съёмка: скромное покрытие, без откровенных поз, ");
            builder.append("только купленная вещь с image2, надета обычным образом. ");
        }
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    public static String moderationContext(String garmentTitle, String garmentCategory, boolean strict) {
        StringBuilder builder = new StringBuilder();
        builder.append("RETAIL VIRTUAL FITTING ROOM (mandatory context): ");
        builder.append("Standard women's apparel e-commerce catalog preview — same as Wildberries/Ozon product pages. ");
        builder.append("Purpose: customer checks fit before buying homewear/sleepwear/dress/outerwear. ");
        builder.append("PG family-safe output only: fully clothed person in the marketplace garment, ");
        builder.append("upright neutral studio pose, professional shopping assistant — NOT adult content, ");
        builder.append("NOT erotic, NOT pornographic, NOT striptease, NOT nudity. ");
        builder.append("Garment: ").append(describeGarment(garmentTitle, garmentCategory)).append(". ");
        if (strict) {
            builder.append("Conservative catalog photography: modest coverage, no suggestive pose, ");
            builder.append("no focus on underwear — only the purchased apparel item from image2 worn normally. ");
        }
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    public static boolean isContentModerationFailure(Throwable error) {
        if (error == null) {
            return false;
        }
        String message = error.getMessage();
        if (message == null) {
            return messageFromCause(error);
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("content moderation")
            || lower.contains("rejected by content moderation")
            || lower.contains("generated image rejected");
    }

    private static boolean messageFromCause(Throwable error) {
        Throwable cause = error.getCause();
        return cause != null && cause != error && isContentModerationFailure(cause);
    }

    private static String describeGarment(String garmentTitle, String garmentCategory) {
        StringBuilder builder = new StringBuilder();
        if (garmentTitle != null && !garmentTitle.isBlank()) {
            builder.append(GarmentTitleSanitizer.forPrompt(garmentTitle.trim()));
        } else {
            builder.append("women's apparel");
        }
        if (garmentCategory != null && !garmentCategory.isBlank()) {
            builder.append(" (").append(translateCategory(garmentCategory)).append(')');
        }
        String combined = builder.toString().toLowerCase(Locale.ROOT);
        if (combined.contains("пеньюар") || combined.contains("peignoir") || combined.contains("nightgown")
            || combined.contains("night") || combined.contains("сороч") || combined.contains("халат")) {
            builder.append(" — обычная домашняя/ночная одежда для магазина, не откровенный контент");
        }
        return builder.toString();
    }

    private static String translateCategory(String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "dress" -> "dress";
            case "top" -> "top";
            case "pants" -> "pants";
            case "jacket" -> "jacket";
            case "shoes" -> "shoes";
            case "accessory" -> "accessory";
            default -> category;
        };
    }
}
