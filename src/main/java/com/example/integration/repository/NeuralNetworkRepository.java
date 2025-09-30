package com.example.integration.repository;

import com.example.integration.model.NeuralNetwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NeuralNetworkRepository extends JpaRepository<NeuralNetwork, UUID> {
    
    Optional<NeuralNetwork> findByName(String name);
    
    List<NeuralNetwork> findByIsActiveTrue();
    
    List<NeuralNetwork> findByNetworkTypeAndIsActiveTrue(String networkType);
    
    @Query("SELECT n FROM NeuralNetwork n WHERE n.isActive = true ORDER BY n.priority ASC, n.isFree DESC")
    List<NeuralNetwork> findActiveOrderedByPriority();
    
    @Query("SELECT n FROM NeuralNetwork n WHERE n.networkType = :type AND n.isActive = true ORDER BY n.priority ASC, n.isFree DESC")
    List<NeuralNetwork> findByTypeOrderedByPriority(String type);
}

