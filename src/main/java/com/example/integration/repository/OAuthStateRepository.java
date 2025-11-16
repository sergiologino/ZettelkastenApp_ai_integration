package com.example.integration.repository;

import com.example.integration.model.OAuthState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthStateRepository extends JpaRepository<OAuthState, UUID> {
    Optional<OAuthState> findByState(String state);
    void deleteByState(String state);
}


