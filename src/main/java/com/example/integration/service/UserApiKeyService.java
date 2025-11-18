package com.example.integration.service;

import com.example.integration.model.ClientApplication;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.model.UserAccount;
import com.example.integration.model.UserApiKey;
import com.example.integration.repository.ClientApplicationRepository;
import com.example.integration.repository.NeuralNetworkRepository;
import com.example.integration.repository.UserApiKeyRepository;
import com.example.integration.repository.UserClientLinkRepository;
import com.example.integration.security.EncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserApiKeyService {
    
    private final UserApiKeyRepository userApiKeyRepository;
    private final ClientApplicationRepository clientApplicationRepository;
    private final NeuralNetworkRepository neuralNetworkRepository;
    private final EncryptionService encryptionService;
    private final SubscriptionService subscriptionService;
    private final UserClientLinkRepository userClientLinkRepository;
    
    public UserApiKeyService(
            UserApiKeyRepository userApiKeyRepository,
            ClientApplicationRepository clientApplicationRepository,
            NeuralNetworkRepository neuralNetworkRepository,
            EncryptionService encryptionService,
            SubscriptionService subscriptionService,
            UserClientLinkRepository userClientLinkRepository) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.clientApplicationRepository = clientApplicationRepository;
        this.neuralNetworkRepository = neuralNetworkRepository;
        this.encryptionService = encryptionService;
        this.subscriptionService = subscriptionService;
        this.userClientLinkRepository = userClientLinkRepository;
    }
    
    /**
     * Проверить, имеет ли пользователь платный тариф (доступ к пользовательским ключам)
     */
    public boolean hasPaidSubscription(UserAccount user) {
        return subscriptionService.getCurrentSubscription(user)
                .map(sub -> !sub.getSubscriptionPlan().isFree())
                .orElse(false);
    }
    
    /**
     * Сохранить или обновить пользовательский API ключ
     */
    public UserApiKey saveApiKey(UserAccount user, UUID clientId, UUID networkId, String apiKey) {
        // Проверяем платный тариф
        if (!hasPaidSubscription(user)) {
            throw new IllegalArgumentException("Использование собственных API ключей доступно только для платного тарифа");
        }
        
        // Проверяем, что клиент принадлежит пользователю
        ClientApplication client = clientApplicationRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Клиентское приложение не найдено"));
        
        // Проверяем, что пользователь владеет клиентом
        boolean ownsClient = userClientLinkRepository.findByClientApplication(clientId)
                .map(link -> link.getUser().getId().equals(user.getId()))
                .orElse(false);
        
        if (!ownsClient) {
            throw new IllegalArgumentException("Клиентское приложение не принадлежит пользователю");
        }
        
        NeuralNetwork network = neuralNetworkRepository.findById(networkId)
                .orElseThrow(() -> new IllegalArgumentException("Нейросеть не найдена"));
        
        // Шифруем ключ
        String encryptedKey = encryptionService.encrypt(apiKey);
        
        // Ищем существующий ключ
        Optional<UserApiKey> existingKey = userApiKeyRepository.findByUserAccountAndClientApplicationAndNeuralNetwork(
                user, client, network);
        
        UserApiKey userApiKey;
        if (existingKey.isPresent()) {
            userApiKey = existingKey.get();
            userApiKey.setApiKeyEncrypted(encryptedKey);
        } else {
            userApiKey = new UserApiKey(user, client, network, encryptedKey);
        }
        
        return userApiKeyRepository.save(userApiKey);
    }
    
    /**
     * Получить пользовательский API ключ (расшифрованный)
     */
    public Optional<String> getApiKey(UserAccount user, UUID clientId, UUID networkId) {
        Optional<UserApiKey> userApiKey = userApiKeyRepository.findByUserAndClientAndNetwork(
                user.getId(), clientId, networkId);
        
        if (userApiKey.isPresent()) {
            try {
                String decrypted = encryptionService.decrypt(userApiKey.get().getApiKeyEncrypted());
                return Optional.of(decrypted);
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Получить пользовательский API ключ по объектам
     */
    public Optional<String> getApiKey(UserAccount user, ClientApplication client, NeuralNetwork network) {
        Optional<UserApiKey> userApiKey = userApiKeyRepository.findByUserAccountAndClientApplicationAndNeuralNetwork(
                user, client, network);
        
        if (userApiKey.isPresent()) {
            try {
                String decrypted = encryptionService.decrypt(userApiKey.get().getApiKeyEncrypted());
                return Optional.of(decrypted);
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Удалить пользовательский API ключ
     */
    public void deleteApiKey(UserAccount user, UUID clientId, UUID networkId) {
        ClientApplication client = clientApplicationRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Клиентское приложение не найдено"));
        
        NeuralNetwork network = neuralNetworkRepository.findById(networkId)
                .orElseThrow(() -> new IllegalArgumentException("Нейросеть не найдена"));
        
        userApiKeyRepository.deleteByUserAccountAndClientApplicationAndNeuralNetwork(user, client, network);
    }
    
    /**
     * Получить все ключи пользователя для конкретного клиента
     */
    public List<UserApiKey> getUserApiKeys(UserAccount user, UUID clientId) {
        ClientApplication client = clientApplicationRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Клиентское приложение не найдено"));
        
        return userApiKeyRepository.findByUserAccountAndClientApplication(user, client);
    }
    
    /**
     * Получить все ключи пользователя
     */
    public List<UserApiKey> getAllUserApiKeys(UserAccount user) {
        return userApiKeyRepository.findByUserAccount(user);
    }
}

