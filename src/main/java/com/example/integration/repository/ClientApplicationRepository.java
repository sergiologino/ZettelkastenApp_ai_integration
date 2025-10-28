package com.example.integration.repository;

import com.example.integration.model.ClientApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientApplicationRepository extends JpaRepository<ClientApplication, UUID> {
    
    Optional<ClientApplication> findByApiKey(String apiKey);
    
    Optional<ClientApplication> findByName(String name);
    
    boolean existsByApiKey(String apiKey);
    
    List<ClientApplication> findByIsActiveTrue();
    
    List<ClientApplication> findByDeletedFalse();
}

