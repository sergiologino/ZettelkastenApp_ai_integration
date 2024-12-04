package com.example.integration.repository;

import com.example.integration.model.NeuralNetworkMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MappingRepository extends JpaRepository<NeuralNetworkMapping, Long> {
    List<NeuralNetworkMapping> findByNeuralNetworkNameAndRequestType(String neuralNetworkName, String requestType);
}
