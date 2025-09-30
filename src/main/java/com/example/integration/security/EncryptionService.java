package com.example.integration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Сервис для шифрования/расшифрования чувствительных данных (API ключей)
 */
@Service
public class EncryptionService {
    
    // Секретный ключ для шифрования (должен быть 16, 24 или 32 байта для AES)
    @Value("${encryption.secret-key:change-this-to-32-byte-secret-key-in-production}")
    private String secretKey;
    
    private static final String ALGORITHM = "AES";
    
    /**
     * Зашифровать строку
     * @param data - данные для шифрования (например, API ключ)
     * @return зашифрованная строка в Base64
     */
    public String encrypt(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        try {
            // Создаём ключ шифрования из секретного ключа
            SecretKeySpec keySpec = createKeySpec();
            
            // Создаём шифровальщик
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            
            // Шифруем данные
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Возвращаем в Base64 для хранения в БД
            return Base64.getEncoder().encodeToString(encrypted);
            
        } catch (Exception e) {
            throw new RuntimeException("Ошибка шифрования: " + e.getMessage(), e);
        }
    }
    
    /**
     * Расшифровать строку
     * @param encryptedData - зашифрованные данные в Base64
     * @return расшифрованная строка
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        try {
            // Создаём ключ шифрования
            SecretKeySpec keySpec = createKeySpec();
            
            // Создаём расшифровщик
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            
            // Декодируем из Base64
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            
            // Расшифровываем
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Ошибка расшифрования: " + e.getMessage(), e);
        }
    }
    
    /**
     * Создать ключ шифрования из секретного ключа
     */
    private SecretKeySpec createKeySpec() {
        // Берём первые 16 байт секретного ключа для AES-128
        // В продакшене используйте 32 байта для AES-256
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16]; // 16 байт = 128 бит
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, key.length));
        
        return new SecretKeySpec(key, ALGORITHM);
    }
}

