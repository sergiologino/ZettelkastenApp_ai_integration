package com.example.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.integration.model.ExternalUser;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.repository.NetworkLimitRepository;
import com.example.integration.repository.NeuralNetworkRepository;
import com.example.integration.repository.UsageCounterRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    @Test
    void fallbackSelectionSkipsPollinationsNetworks() {
        UsageCounterRepository usageCounterRepository = mock(UsageCounterRepository.class);
        NetworkLimitRepository networkLimitRepository = mock(NetworkLimitRepository.class);
        NeuralNetworkRepository neuralNetworkRepository = mock(NeuralNetworkRepository.class);
        RateLimitService service = new RateLimitService(
            usageCounterRepository,
            networkLimitRepository,
            neuralNetworkRepository
        );
        NeuralNetwork pollinations = freeActiveNetwork("pollinations");

        when(neuralNetworkRepository.findByTypeOrderedByPriority("image_generation"))
            .thenReturn(List.of(pollinations));

        assertThat(service.findFallbackNetwork(new ExternalUser(), "image_generation")).isEmpty();
        verify(networkLimitRepository, never()).findByNeuralNetworkAndUserTypeAndLimitPeriod(any(), any(), any());
    }

    private static NeuralNetwork freeActiveNetwork(String provider) {
        NeuralNetwork network = new NeuralNetwork();
        network.setProvider(provider);
        network.setIsFree(true);
        network.setIsActive(true);
        return network;
    }
}
