package com.example.integration.controller;

import com.example.integration.dto.subscription.*;
import com.example.integration.model.PaymentHistory;
import com.example.integration.model.SubscriptionPlan;
import com.example.integration.model.UserAccount;
import com.example.integration.service.PaymentService;
import com.example.integration.service.SubscriptionService;
import com.example.integration.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для управления подписками
 */
@RestController
@RequestMapping("/api/user/subscriptions")
@SecurityRequirement(name = "Bearer")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final UserContextService userContextService;

    public SubscriptionController(SubscriptionService subscriptionService,
                                 PaymentService paymentService,
                                 UserContextService userContextService) {
        this.subscriptionService = subscriptionService;
        this.paymentService = paymentService;
        this.userContextService = userContextService;
    }

    /**
     * Получить все доступные тарифные планы
     */
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanDto>> getAllPlans() {
        try {
            List<SubscriptionPlan> plans = subscriptionService.getAllPlans();
            List<SubscriptionPlanDto> planDTOs = plans.stream()
                    .map(SubscriptionPlanDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(planDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить информацию о текущей подписке пользователя
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSubscription(HttpServletRequest request) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    Optional<com.example.integration.model.Subscription> subscriptionOpt = 
                        subscriptionService.getCurrentSubscription(user);
                    
                    if (subscriptionOpt.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("message", "Подписка не найдена"));
                    }
                    
                    SubscriptionInfoDto dto = SubscriptionInfoDto.fromEntity(subscriptionOpt.get());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Создать новую подписку
     */
    @PostMapping("/create")
    public ResponseEntity<?> createSubscription(HttpServletRequest request,
                                                @RequestBody CreateSubscriptionRequest req) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    try {
                        Optional<SubscriptionPlan> planOpt = subscriptionService.getPlanByName(req.getPlanName());
                        if (planOpt.isEmpty()) {
                            return ResponseEntity.badRequest()
                                    .body(Map.of("error", "План не найден"));
                        }
                        
                        SubscriptionPlan plan = planOpt.get();
                        
                        // Если план бесплатный, сразу создаем подписку
                        if (plan.isFree()) {
                            com.example.integration.model.Subscription subscription = 
                                subscriptionService.createSubscription(user, plan);
                            
                            CreateSubscriptionResponse response = new CreateSubscriptionResponse();
                            response.setSuccess(true);
                            response.setMessage("Подписка активирована");
                            response.setSubscription(SubscriptionInfoDto.fromEntity(subscription));
                            return ResponseEntity.ok(response);
                        }
                        
                        // Для платных планов создаем платеж
                        PaymentHistory.PaymentMethod paymentMethod;
                        try {
                            paymentMethod = PaymentHistory.PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return ResponseEntity.badRequest()
                                    .body(Map.of("error", "Неверный метод оплаты"));
                        }
                        
                        PaymentHistory payment = paymentService.createPayment(user, plan, paymentMethod);
                        PaymentService.PaymentInitiationResult result = paymentService.initiatePayment(payment);
                        
                        CreateSubscriptionResponse response = new CreateSubscriptionResponse();
                        response.setSuccess(result.isSuccess());
                        response.setMessage(result.getMessage());
                        response.setPaymentUrl(result.getPaymentUrl());
                        
                        return ResponseEntity.ok(response);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Отменить подписку
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(HttpServletRequest request,
                                                @RequestBody(required = false) Map<String, String> body) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    String reason = body != null ? body.get("reason") : "Отменено пользователем";
                    subscriptionService.cancelSubscription(user, reason);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Подписка отменена"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Получить историю платежей
     */
    @GetMapping("/payments/history")
    public ResponseEntity<?> getPaymentHistory(HttpServletRequest request) {
        return userContextService.resolveCurrentUser(request)
                .<ResponseEntity<?>>map(user -> {
                    List<PaymentHistory> payments = paymentService.getUserPayments(user);
                    // TODO: создать PaymentHistoryDto если нужно
                    return ResponseEntity.ok(payments);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Получить статус платежа
     */
    @GetMapping("/payment/{transactionId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String transactionId) {
        Optional<PaymentHistory> paymentOpt = paymentService.getPaymentByTransactionId(transactionId);
        
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Платеж не найден"));
        }
        
        PaymentHistory payment = paymentOpt.get();
        return ResponseEntity.ok(Map.of(
            "transactionId", payment.getTransactionId(),
            "status", payment.getStatus().name(),
            "amount", payment.getAmount(),
            "currency", payment.getCurrency(),
            "createdAt", payment.getCreatedAt(),
            "completedAt", payment.getCompletedAt()
        ));
    }
}

