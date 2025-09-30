package com.example.integration.repository;

import com.example.integration.model.ExternalUser;
import com.example.integration.model.NeuralNetwork;
import com.example.integration.model.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {
    
    Optional<UsageCounter> findByExternalUserAndNeuralNetworkAndPeriodStart(
        ExternalUser externalUser,
        NeuralNetwork neuralNetwork,
        LocalDate periodStart
    );
    
    @Query("SELECT uc FROM UsageCounter uc WHERE uc.externalUser = :user " +
           "AND uc.periodStart <= :date AND uc.periodEnd >= :date")
    List<UsageCounter> findActiveCountersForUser(ExternalUser user, LocalDate date);
    
    @Query("SELECT uc FROM UsageCounter uc WHERE uc.externalUser = :user " +
           "AND uc.neuralNetwork = :network " +
           "AND uc.periodStart <= :date AND uc.periodEnd >= :date")
    Optional<UsageCounter> findActiveCounterForUserAndNetwork(
        ExternalUser user, 
        NeuralNetwork network, 
        LocalDate date
    );
}

