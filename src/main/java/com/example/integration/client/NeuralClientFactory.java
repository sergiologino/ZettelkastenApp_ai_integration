package com.example.integration.client;

import com.example.integration.model.NeuralNetwork;
import org.springframework.stereotype.Component;

/**
 * Фабрика для выбора нужного клиента нейросети
 */
@Component
public class NeuralClientFactory {
    
    private final OpenAiClient openAiClient;
    private final YandexGptClient yandexGptClient;
    private final ClaudeClient claudeClient;
    private final MistralClient mistralClient;
    private final GigaChatClient gigaChatClient;
    private final WhisperClient whisperClient;
    private final QwenClient qwenClient;
    private final DeepSeekClient deepSeekClient;
    private final PollinationsClient pollinationsClient;
    private final VirtualTryOnClient virtualTryOnClient;
    
    public NeuralClientFactory(
        OpenAiClient openAiClient,
        YandexGptClient yandexGptClient,
        ClaudeClient claudeClient,
        MistralClient mistralClient,
        GigaChatClient gigaChatClient,
        WhisperClient whisperClient,
        QwenClient qwenClient,
        DeepSeekClient deepSeekClient,
        PollinationsClient pollinationsClient,
        VirtualTryOnClient virtualTryOnClient
    ) {
        this.openAiClient = openAiClient;
        this.yandexGptClient = yandexGptClient;
        this.claudeClient = claudeClient;
        this.mistralClient = mistralClient;
        this.gigaChatClient = gigaChatClient;
        this.whisperClient = whisperClient;
        this.qwenClient = qwenClient;
        this.deepSeekClient = deepSeekClient;
        this.pollinationsClient = pollinationsClient;
        this.virtualTryOnClient = virtualTryOnClient;
    }
    
    /**
     * Получить клиент для нейросети
     */
    public BaseNeuralClient getClient(NeuralNetwork network) {
        String provider = network.getProvider().toLowerCase();
        
        return switch (provider) {
            case "openai" -> openAiClient;
            case "yandex" -> yandexGptClient;
            case "anthropic", "claude" -> claudeClient;
            case "mistral" -> mistralClient;
            case "sber", "gigachat" -> gigaChatClient;
            case "whisper" -> whisperClient;
            case "qwen" -> qwenClient;
            case "deepseek" -> deepSeekClient;
            case "pollinations" -> pollinationsClient;
            case "virtual_try_on", "virtual-try-on", "wibestyle" -> virtualTryOnClient;
            case "google", "xai" -> openAiClient;
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }
}

