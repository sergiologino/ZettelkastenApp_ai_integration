package com.example.integration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TryOnImageSupportTest {

    @Test
    void toKlingImageStripsDataUriPrefix() {
        String raw = TryOnImageSupport.toKlingImage("data:image/jpeg;base64,abc123");
        assertEquals("abc123", raw);
    }

    @Test
    void toFashnImageKeepsDataUri() {
        String value = TryOnImageSupport.toFashnImage("abc123");
        assertTrue(value.startsWith("data:image/jpeg;base64,"));
    }

    @Test
    void toFashnImageKeepsHttpUrl() {
        String url = "https://example.com/garment.jpg";
        assertEquals(url, TryOnImageSupport.toFashnImage(url));
    }
}
