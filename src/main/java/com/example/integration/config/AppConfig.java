package com.example.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class AppConfig {

    @Bean
    public NeuralNetworkConfig neuralNetworkConfig() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Загружаем JSON из файла
        ClassPathResource resource = new ClassPathResource("neural_networks_config.json");
        return objectMapper.readValue(resource.getInputStream(), NeuralNetworkConfig.class);
    }
}

