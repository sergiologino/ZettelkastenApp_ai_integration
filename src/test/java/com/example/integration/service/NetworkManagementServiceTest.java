package com.example.integration.service;

import com.example.integration.dto.NetworkCreateRequest;
import com.example.integration.dto.NetworkDTO;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.repository.NeuralNetworkRepository;
import com.example.integration.security.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkManagementServiceTest {

    @Mock
    private NeuralNetworkRepository repository;

    @Mock
    private EncryptionService encryptionService;

    private NetworkManagementService service;

    @BeforeEach
    void setUp() {
        service = new NetworkManagementService(repository, encryptionService);
    }

    @Test
    void updateWithBlankCredentialsKeepsStoredKlingKeys() {
        UUID id = UUID.randomUUID();
        NeuralNetwork network = network(id);
        network.setApiKeyEncrypted("encrypted-access-key");
        network.setApiSecretEncrypted("encrypted-secret-key");
        NetworkCreateRequest request = request();
        request.setApiKey("   ");
        request.setApiSecret("");

        when(repository.findById(id)).thenReturn(Optional.of(network));
        when(repository.save(any(NeuralNetwork.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NetworkDTO result = service.updateNetwork(id, request);

        assertThat(network.getApiKeyEncrypted()).isEqualTo("encrypted-access-key");
        assertThat(network.getApiSecretEncrypted()).isEqualTo("encrypted-secret-key");
        assertThat(result.getHasApiKey()).isTrue();
        assertThat(result.getHasApiSecret()).isTrue();
        verify(encryptionService, never()).encrypt(any());
    }

    @Test
    void updateEncryptsAndStoresBothTrimmedKlingKeys() {
        UUID id = UUID.randomUUID();
        NeuralNetwork network = network(id);
        NetworkCreateRequest request = request();
        request.setApiKey(" access-key ");
        request.setApiSecret(" secret-key ");

        when(repository.findById(id)).thenReturn(Optional.of(network));
        when(encryptionService.encrypt("access-key")).thenReturn("encrypted-access-key");
        when(encryptionService.encrypt("secret-key")).thenReturn("encrypted-secret-key");
        when(repository.save(any(NeuralNetwork.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NetworkDTO result = service.updateNetwork(id, request);

        assertThat(network.getApiKeyEncrypted()).isEqualTo("encrypted-access-key");
        assertThat(network.getApiSecretEncrypted()).isEqualTo("encrypted-secret-key");
        assertThat(result.getHasApiKey()).isTrue();
        assertThat(result.getHasApiSecret()).isTrue();
        verify(encryptionService).encrypt("access-key");
        verify(encryptionService).encrypt("secret-key");
    }

    private static NeuralNetwork network(UUID id) {
        NeuralNetwork network = new NeuralNetwork();
        network.setId(id);
        return network;
    }

    private static NetworkCreateRequest request() {
        NetworkCreateRequest request = new NetworkCreateRequest();
        request.setName("kling-kolors-tryon");
        request.setDisplayName("Kling Try-On Photo");
        request.setProvider("kling");
        request.setNetworkType("image_generation");
        request.setApiUrl("https://api.klingai.com");
        request.setModelName("kolors-virtual-try-on-v1");
        request.setIsActive(true);
        request.setIsFree(false);
        return request;
    }
}
