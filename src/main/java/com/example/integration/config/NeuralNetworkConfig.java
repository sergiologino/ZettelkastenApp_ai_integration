package com.example.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

@Data
public class NeuralNetworkConfig {
    private Map<String, NeuralNetworkProperties> networks;

    public Map<String, NeuralNetworkProperties> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, NeuralNetworkProperties> networks) {
        this.networks = networks;
    }

    @Data
    public static class NeuralNetworkProperties {
        private String url;
        private String apiKey;
    }

    @Bean
    public NeuralNetworkConfig neuralNetworkConfig() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Загружаем JSON из файла
        ClassPathResource resource = new ClassPathResource("neural_networks_config.json");
        return objectMapper.readValue(resource.getInputStream(), NeuralNetworkConfig.class);
    }
}
