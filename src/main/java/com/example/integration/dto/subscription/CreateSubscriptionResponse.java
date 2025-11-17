package com.example.integration.dto.subscription;

import lombok.Data;

@Data
public class CreateSubscriptionResponse {
    private Boolean success;
    private String message;
    private String paymentUrl;
    private SubscriptionInfoDto subscription;
}

