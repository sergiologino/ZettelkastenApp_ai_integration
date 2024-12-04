package com.example.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NeuralNetworkConfigService {

    private NeuralNetworkConfig config;

    public void loadConfig() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Загрузка файла из resources
        ClassPathResource resource = new ClassPathResource("neural_networks_config.json");
        this.config = objectMapper.readValue(resource.getInputStream(), NeuralNetworkConfig.class);
    }

    public NeuralNetworkConfig.NeuralNetworkProperties getNetworkProperties(String neuralNetworkName) {
        if (config == null) {
            throw new IllegalStateException("Конфигурация не загружена. Вызовите loadConfig() перед использованием.");
        }
        return config.getNetworks().get(neuralNetworkName);
    }
}
