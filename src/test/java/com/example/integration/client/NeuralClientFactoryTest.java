package com.example.integration.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.integration.model.NeuralNetwork;
import org.junit.jupiter.api.Test;

class NeuralClientFactoryTest {

    @Test
    void pollinationsProviderIsNotSupported() {
        NeuralClientFactory factory = new NeuralClientFactory(
            mock(OpenAiClient.class),
            mock(YandexGptClient.class),
            mock(ClaudeClient.class),
            mock(MistralClient.class),
            mock(GigaChatClient.class),
            mock(WhisperClient.class),
            mock(QwenClient.class),
            mock(DeepSeekClient.class),
            mock(VirtualTryOnClient.class),
            mock(FashnClient.class),
            mock(KlingVirtualTryOnClient.class),
            mock(SeasonHitVideoClient.class)
        );
        NeuralNetwork network = new NeuralNetwork();
        network.setProvider("pollinations");

        assertThatThrownBy(() -> factory.getClient(network))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown provider: pollinations");
    }
}
