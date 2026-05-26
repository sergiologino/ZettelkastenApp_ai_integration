package com.example.integration.support;

import com.example.integration.model.NeuralNetwork;
import com.example.integration.model.SystemSetting;
import com.example.integration.repository.SystemSettingRepository;
import com.example.integration.security.EncryptionService;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves xAI API key for Grok Imagine: network encrypted key first, then env / system settings.
 */
@Component
public class XaiApiKeyResolver {

    private static final String[] SETTING_KEYS = {"XAI_API_KEY", "xai_api_key", "xai.api-key"};

    private final EncryptionService encryptionService;
    private final SystemSettingRepository systemSettingRepository;
    private final String envApiKey;

    public XaiApiKeyResolver(
        EncryptionService encryptionService,
        SystemSettingRepository systemSettingRepository,
        @Value("${XAI_API_KEY:}") String envApiKey,
        @Value("${xai.api-key:}") String legacyEnvApiKey
    ) {
        this.encryptionService = encryptionService;
        this.systemSettingRepository = systemSettingRepository;
        String primary = envApiKey != null && !envApiKey.isBlank() ? envApiKey : legacyEnvApiKey;
        this.envApiKey = primary != null ? primary.trim() : "";
    }

    public Optional<String> resolve(NeuralNetwork network) {
        if (network != null && network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isBlank()) {
            String decrypted = encryptionService.decrypt(network.getApiKeyEncrypted());
            if (decrypted != null && !decrypted.isBlank()) {
                return Optional.of(decrypted.trim());
            }
        }
        if (!envApiKey.isBlank()) {
            return Optional.of(envApiKey);
        }
        for (String key : SETTING_KEYS) {
            Optional<SystemSetting> setting = systemSettingRepository.findByKey(key);
            if (setting.isPresent()) {
                String value = setting.get().getValue();
                if (value != null && !value.isBlank()) {
                    return Optional.of(value.trim());
                }
            }
        }
        return Optional.empty();
    }

    public String describeKeySource(NeuralNetwork network) {
        if (network != null && network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isBlank()) {
            String decrypted = encryptionService.decrypt(network.getApiKeyEncrypted());
            if (decrypted != null && !decrypted.isBlank()) {
                return "network_api_key_encrypted";
            }
        }
        if (!envApiKey.isBlank()) {
            return "env_XAI_API_KEY";
        }
        for (String key : SETTING_KEYS) {
            if (systemSettingRepository.findByKey(key).map(SystemSetting::getValue).filter(v -> v != null && !v.isBlank()).isPresent()) {
                return "system_setting_" + key.toLowerCase(Locale.ROOT);
            }
        }
        return "none";
    }
}
