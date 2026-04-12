package com.example.integration.dto.subscription;

import lombok.Data;

@Data
public class CreateSubscriptionRequest {
    private String planName;
    private String paymentMethod; // CARD, MOBILE_PAYMENT, BANK_TRANSFER, E_WALLET
}

