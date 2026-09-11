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
    private final VirtualTryOnClient virtualTryOnClient;
    private final FashnClient fashnClient;
    private final KlingVirtualTryOnClient klingVirtualTryOnClient;
    private final SeasonHitVideoClient seasonHitVideoClient;
    
    public NeuralClientFactory(
        OpenAiClient openAiClient,
        YandexGptClient yandexGptClient,
        ClaudeClient claudeClient,
        MistralClient mistralClient,
        GigaChatClient gigaChatClient,
        WhisperClient whisperClient,
        QwenClient qwenClient,
        DeepSeekClient deepSeekClient,
        VirtualTryOnClient virtualTryOnClient,
        FashnClient fashnClient,
        KlingVirtualTryOnClient klingVirtualTryOnClient,
        SeasonHitVideoClient seasonHitVideoClient
    ) {
        this.openAiClient = openAiClient;
        this.yandexGptClient = yandexGptClient;
        this.claudeClient = claudeClient;
        this.mistralClient = mistralClient;
        this.gigaChatClient = gigaChatClient;
        this.whisperClient = whisperClient;
        this.qwenClient = qwenClient;
        this.deepSeekClient = deepSeekClient;
        this.virtualTryOnClient = virtualTryOnClient;
        this.fashnClient = fashnClient;
        this.klingVirtualTryOnClient = klingVirtualTryOnClient;
        this.seasonHitVideoClient = seasonHitVideoClient;
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
            case "virtual_try_on", "virtual-try-on", "wibestyle" -> virtualTryOnClient;
            case "fashn" -> fashnClient;
            case "kling", "kling_vton", "kling-vton" -> klingVirtualTryOnClient;
            case "season_hit_video", "season-hit-video", "wibestyle_video" -> seasonHitVideoClient;
            case "google", "xai" -> openAiClient;
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }
}

