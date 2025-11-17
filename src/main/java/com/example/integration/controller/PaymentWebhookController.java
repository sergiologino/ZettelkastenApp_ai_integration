package com.example.integration.controller;

import com.example.integration.service.PaymentService;
import com.example.integration.service.YooKassaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Контроллер для обработки webhook от платежных провайдеров
 */
@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {

    private final YooKassaService yooKassaService;
    private final PaymentService paymentService;

    public PaymentWebhookController(YooKassaService yooKassaService, PaymentService paymentService) {
        this.yooKassaService = yooKassaService;
        this.paymentService = paymentService;
    }

    /**
     * Webhook от ЮКассы
     */
    @PostMapping("/yookassa")
    public ResponseEntity<?> yooKassaWebhook(@RequestBody String requestBody) {
        try {
            YooKassaService.WebhookValidationResult validation = yooKassaService.validateWebhook(requestBody);
            
            if (!validation.isSuccess()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", validation.getError()));
            }
            
            // Обрабатываем webhook
            String transactionId = validation.getTransactionId();
            if (transactionId == null) {
                transactionId = validation.getPaymentId(); // Fallback на paymentId
            }
            
            paymentService.processWebhook(transactionId, validation.getStatus(), validation.getRawData());
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

