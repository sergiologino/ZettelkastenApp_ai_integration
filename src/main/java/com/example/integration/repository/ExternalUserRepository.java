package com.example.integration.repository;

import com.example.integration.model.ClientApplication;
import com.example.integration.model.ExternalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalUserRepository extends JpaRepository<ExternalUser, UUID> {
    
    Optional<ExternalUser> findByClientAppAndExternalUserId(ClientApplication clientApp, String externalUserId);
    
    boolean existsByClientAppAndExternalUserId(ClientApplication clientApp, String externalUserId);
}

