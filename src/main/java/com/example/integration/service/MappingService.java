package com.example.integration.service;

import com.example.integration.model.NeuralNetworkMapping;
import com.example.integration.repository.MappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MappingService {


    private final MappingRepository mappingRepository;

    public Map<String, Object> buildRequestPayload(String neuralNetworkName, String requestType, Map<String, Object> sourcePayload) {
        List<NeuralNetworkMapping> mappings = mappingRepository.findByNeuralNetworkNameAndRequestType(neuralNetworkName, requestType);
        Map<String, Object> targetPayload = new HashMap<>();

        for (NeuralNetworkMapping mapping : mappings) {
            Object value = sourcePayload.get(mapping.getSourceAttribute());
            if (value != null) {
                targetPayload.put(mapping.getTargetAttribute(), value);
            }
        }

        return targetPayload;
    }
}
