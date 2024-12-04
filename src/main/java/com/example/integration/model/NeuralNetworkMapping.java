package com.example.integration.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "neural_network_mappings")
@Data
public class NeuralNetworkMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String neuralNetworkName;
    private String requestType;
    private String sourceAttribute;
    private String targetAttribute;
}
