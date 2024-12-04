package com.example.integration.config;

import com.example.integration.config.NeuralNetworkConfigService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ConfigInitializer {

    private final NeuralNetworkConfigService configService;

    public ConfigInitializer(NeuralNetworkConfigService configService) {
        this.configService = configService;
    }

    @PostConstruct
    public void init() {
        try {
            configService.loadConfig();
            System.out.println("Конфигурация нейросетей загружена успешно!");
        } catch (IOException e) {
            System.err.println("Ошибка загрузки конфигурации нейросетей: " + e.getMessage());
        }
    }
}
