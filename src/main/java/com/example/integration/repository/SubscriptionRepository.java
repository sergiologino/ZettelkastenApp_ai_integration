package com.example.integration.repository;

import com.example.integration.model.Subscription;
import com.example.integration.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    
    Optional<Subscription> findFirstByUserAccountAndStatusOrderByStartedAtDesc(
        UserAccount userAccount, 
        Subscription.SubscriptionStatus status
    );
    
    @Query("SELECT s FROM Subscription s WHERE s.userAccount = :user AND s.status = 'ACTIVE' AND (s.expiresAt IS NULL OR s.expiresAt > :now) ORDER BY s.startedAt DESC")
    Optional<Subscription> findCurrentActiveSubscription(@Param("user") UserAccount user, @Param("now") LocalDateTime now);
    
    List<Subscription> findByUserAccountOrderByCreatedAtDesc(UserAccount userAccount);
    
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.expiresAt IS NOT NULL AND s.expiresAt < :now")
    List<Subscription> findExpiredActiveSubscriptions(@Param("now") LocalDateTime now);
}

