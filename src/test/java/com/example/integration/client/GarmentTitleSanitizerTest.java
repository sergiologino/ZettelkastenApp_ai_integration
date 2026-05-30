package com.example.integration.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GarmentTitleSanitizerTest {

    @Test
    void replacesSexualAdjectiveDeclensions() {
        assertEquals("Стильное платье миди", GarmentTitleSanitizer.forPrompt("Сексуальное платье миди"));
        assertEquals("стильная блузка", GarmentTitleSanitizer.forPrompt("сексуальная блузка"));
    }

    @Test
    void replacesEroticOutfitPhrase() {
        assertEquals("повседневный наряд", GarmentTitleSanitizer.forPrompt("эротический наряд"));
        assertEquals("повседневная сорочка", GarmentTitleSanitizer.forPrompt("эротическая сорочка"));
    }
}
