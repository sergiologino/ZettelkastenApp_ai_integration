package com.example.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(TtsProperties.class)
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(60))
            .setReadTimeout(Duration.ofSeconds(120))
            .build();
    }

    @Bean
    @Qualifier("ttsRestTemplate")
    public RestTemplate ttsRestTemplate(RestTemplateBuilder builder, TtsProperties ttsProperties) {
        return builder
            .setConnectTimeout(Duration.ofMillis(ttsProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(ttsProperties.getReadTimeoutMs()))
            .build();
    }
}

