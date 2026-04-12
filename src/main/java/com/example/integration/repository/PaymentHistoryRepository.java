package com.example.integration.repository;

import com.example.integration.model.PaymentHistory;
import com.example.integration.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {
    
    Optional<PaymentHistory> findByTransactionId(String transactionId);
    
    Optional<PaymentHistory> findByProviderTransactionId(String providerTransactionId);
    
    List<PaymentHistory> findByUserAccountOrderByCreatedAtDesc(UserAccount userAccount);
}

