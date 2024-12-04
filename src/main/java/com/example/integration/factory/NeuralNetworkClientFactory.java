package com.example.integration.factory;

import com.example.integration.config.NeuralNetworkConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component

public class NeuralNetworkClientFactory {

    private final NeuralNetworkConfig neuralNetworkConfig;
    private final RestTemplate restTemplate;

    public NeuralNetworkClientFactory(NeuralNetworkConfig neuralNetworkConfig) {
        this.neuralNetworkConfig = neuralNetworkConfig;
        this.restTemplate = new RestTemplate();
    }

    public NeuralNetworkClient getClient(String neuralNetworkName) {
        NeuralNetworkConfig.NeuralNetworkProperties properties = neuralNetworkConfig.getNetworks().get(neuralNetworkName);

        if (properties == null) {
            throw new IllegalArgumentException("Unknown neural network: " + neuralNetworkName);
        }

        return new NeuralNetworkClient(properties.getUrl(), properties.getApiKey(), restTemplate);
    }

}

