package com.example.integration.repository;

import com.example.integration.model.ClientApplication;
import com.example.integration.model.ClientNetworkAccess;
import com.example.integration.model.NeuralNetwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления доступом клиентов к нейросетям
 */
@Repository
public interface ClientNetworkAccessRepository extends JpaRepository<ClientNetworkAccess, UUID>, JpaSpecificationExecutor<ClientNetworkAccess> {

    /**
     * Найти доступ по клиенту и нейросети
     */
    Optional<ClientNetworkAccess> findByClientApplicationAndNeuralNetwork(ClientApplication clientApplication, NeuralNetwork neuralNetwork);

    /**
     * Найти доступ по ID клиента и ID нейросети
     */
    @Query("SELECT cna FROM ClientNetworkAccess cna WHERE cna.clientApplication.id = :clientId AND cna.neuralNetwork.id = :networkId")
    Optional<ClientNetworkAccess> findByClientIdAndNetworkId(@Param("clientId") UUID clientId, @Param("networkId") UUID networkId);

    /**
     * Получить все доступы для конкретного клиента
     */
    List<ClientNetworkAccess> findByClientApplicationOrderByNeuralNetworkDisplayNameAsc(ClientApplication clientApplication);

    /**
     * Получить все доступы для конкретной нейросети
     */
    List<ClientNetworkAccess> findByNeuralNetworkOrderByClientApplicationNameAsc(NeuralNetwork neuralNetwork);

    /**
     * Получить все доступы с пагинацией
     */
    @Query("SELECT cna FROM ClientNetworkAccess cna ORDER BY cna.clientApplication.name ASC, cna.neuralNetwork.displayName ASC")
    List<ClientNetworkAccess> findAllOrderedByClientAndNetwork();

    /**
     * Проверить существование доступа
     */
    boolean existsByClientApplicationAndNeuralNetwork(ClientApplication clientApplication, NeuralNetwork neuralNetwork);

    /**
     * Проверить существование доступа по ID
     */
    @Query("SELECT COUNT(cna) > 0 FROM ClientNetworkAccess cna WHERE cna.clientApplication.id = :clientId AND cna.neuralNetwork.id = :networkId")
    boolean existsByClientIdAndNetworkId(@Param("clientId") UUID clientId, @Param("networkId") UUID networkId);

    /**
     * Получить количество доступов для клиента
     */
    long countByClientApplication(ClientApplication clientApplication);

    /**
     * Получить количество доступов для нейросети
     */
    long countByNeuralNetwork(NeuralNetwork neuralNetwork);

    /**
     * Получить доступы с лимитами
     */
    @Query("SELECT cna FROM ClientNetworkAccess cna WHERE cna.dailyRequestLimit IS NOT NULL OR cna.monthlyRequestLimit IS NOT NULL")
    List<ClientNetworkAccess> findWithLimits();

    /**
     * Получить доступы без лимитов (неограниченные)
     */
    @Query("SELECT cna FROM ClientNetworkAccess cna WHERE cna.dailyRequestLimit IS NULL AND cna.monthlyRequestLimit IS NULL")
    List<ClientNetworkAccess> findWithoutLimits();

    /**
     * Удалить все доступы для клиента
     */
    void deleteByClientApplication(ClientApplication clientApplication);

    /**
     * Удалить все доступы для нейросети
     */
    void deleteByNeuralNetwork(NeuralNetwork neuralNetwork);
}
