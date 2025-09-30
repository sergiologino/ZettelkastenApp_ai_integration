package com.example.integration.repository;

import com.example.integration.model.NetworkLimit;
import com.example.integration.model.NeuralNetwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NetworkLimitRepository extends JpaRepository<NetworkLimit, UUID> {
    
    Optional<NetworkLimit> findByNeuralNetworkAndUserTypeAndLimitPeriod(
        NeuralNetwork neuralNetwork, 
        String userType, 
        String limitPeriod
    );
    
    List<NetworkLimit> findByNeuralNetwork(NeuralNetwork neuralNetwork);
    
    List<NetworkLimit> findByUserType(String userType);
}

