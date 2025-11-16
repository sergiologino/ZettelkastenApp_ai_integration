package com.example.integration.repository;

import com.example.integration.model.UserClientLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserClientLinkRepository extends JpaRepository<UserClientLink, UUID> {

    @Query("select l from UserClientLink l join fetch l.clientApplication ca where l.user.id = :userId and ca.deleted = false")
    List<UserClientLink> findActiveByUserId(@Param("userId") UUID userId);

    @Query("select l from UserClientLink l join fetch l.clientApplication ca where l.user.id = :userId and ca.id = :clientId")
    Optional<UserClientLink> findByUserAndClient(@Param("userId") UUID userId, @Param("clientId") UUID clientId);
}


