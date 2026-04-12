package com.example.integration.repository;

import com.example.integration.model.ClientApplication;
import com.example.integration.model.ExternalUser;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.model.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, UUID> {
    
    Page<RequestLog> findByClientAppOrderByCreatedAtDesc(ClientApplication clientApp, Pageable pageable);
    
    Page<RequestLog> findByExternalUserOrderByCreatedAtDesc(ExternalUser externalUser, Pageable pageable);
    
    Page<RequestLog> findByNeuralNetworkOrderByCreatedAtDesc(NeuralNetwork neuralNetwork, Pageable pageable);
    
    Page<RequestLog> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    @Query("SELECT rl FROM RequestLog rl WHERE rl.createdAt >= :from AND rl.createdAt <= :to " +
           "ORDER BY rl.createdAt DESC")
    Page<RequestLog> findByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.externalUser = :user " +
           "AND rl.createdAt >= :from AND rl.status = 'success'")
    Long countSuccessfulRequestsForUserSince(ExternalUser user, LocalDateTime from);
    
    // Statistics methods
    long countByStatus(String status);
    
    @Query("SELECT SUM(rl.tokensUsed) FROM RequestLog rl WHERE rl.tokensUsed IS NOT NULL")
    Long sumTokensUsed();
    
    // Статистика по клиенту и нейросети
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.clientApp.id = :clientId AND rl.neuralNetwork.id = :networkId")
    Long countByClientAndNetwork(UUID clientId, UUID networkId);
    
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.clientApp.id = :clientId AND rl.neuralNetwork.id = :networkId AND rl.status = 'success'")
    Long countSuccessfulByClientAndNetwork(UUID clientId, UUID networkId);
    
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.clientApp.id = :clientId AND rl.neuralNetwork.id = :networkId AND rl.status = 'failed'")
    Long countFailedByClientAndNetwork(UUID clientId, UUID networkId);
    
    @Query("SELECT COALESCE(SUM(rl.tokensUsed), 0) FROM RequestLog rl WHERE rl.clientApp.id = :clientId AND rl.neuralNetwork.id = :networkId AND rl.tokensUsed IS NOT NULL")
    Long sumTokensByClientAndNetwork(UUID clientId, UUID networkId);
    
    // Статистика по клиенту (все нейросети) - только для сетей, которые не NULL
    @Query("SELECT rl.neuralNetwork.id, COUNT(rl) FROM RequestLog rl WHERE rl.clientApp.id = :clientId AND rl.neuralNetwork IS NOT NULL GROUP BY rl.neuralNetwork.id")
    List<Object[]> countByClientGroupedByNetwork(UUID clientId);
    
    // Подсчет бесплатных запросов (для бесплатного плана)
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.clientApp.id = :clientId AND rl.neuralNetwork.id = :networkId AND rl.status = 'success'")
    Long countByClientAndNetworkAndFreePlan(UUID clientId, UUID networkId);
    
    // Подсчет запросов в диапазоне дат (для дневных/месячных лимитов)
    @Query("SELECT COUNT(rl) FROM RequestLog rl WHERE rl.clientApp.id = :clientId AND rl.neuralNetwork.id = :networkId AND rl.status = 'success' AND rl.createdAt >= :from AND rl.createdAt <= :to")
    Long countByClientAndNetworkAndDateRange(UUID clientId, UUID networkId, LocalDateTime from, LocalDateTime to);
}

