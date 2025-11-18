package com.example.integration.repository;

import com.example.integration.model.ClientApplication;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.model.UserAccount;
import com.example.integration.model.UserApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, UUID> {
    
    /**
     * Найти ключ по пользователю, клиенту и нейросети
     */
    Optional<UserApiKey> findByUserAccountAndClientApplicationAndNeuralNetwork(
        UserAccount userAccount,
        ClientApplication clientApplication,
        NeuralNetwork neuralNetwork
    );
    
    /**
     * Найти ключ по ID пользователя, клиента и нейросети
     */
    @Query("SELECT uak FROM UserApiKey uak WHERE uak.userAccount.id = :userId " +
           "AND uak.clientApplication.id = :clientId AND uak.neuralNetwork.id = :networkId")
    Optional<UserApiKey> findByUserAndClientAndNetwork(
        @Param("userId") UUID userId,
        @Param("clientId") UUID clientId,
        @Param("networkId") UUID networkId
    );
    
    /**
     * Получить все ключи пользователя для конкретного клиента
     */
    List<UserApiKey> findByUserAccountAndClientApplication(
        UserAccount userAccount,
        ClientApplication clientApplication
    );
    
    /**
     * Получить все ключи пользователя
     */
    List<UserApiKey> findByUserAccount(UserAccount userAccount);
    
    /**
     * Удалить ключ по пользователю, клиенту и нейросети
     */
    void deleteByUserAccountAndClientApplicationAndNeuralNetwork(
        UserAccount userAccount,
        ClientApplication clientApplication,
        NeuralNetwork neuralNetwork
    );
}

