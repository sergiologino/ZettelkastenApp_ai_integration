package com.example.integration.service;

import com.example.integration.model.PaymentHistory;
import com.example.integration.model.SubscriptionPlan;
import com.example.integration.model.UserAccount;
import com.example.integration.repository.PaymentHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления платежами
 */
@Service
@Transactional
public class PaymentService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final YooKassaService yooKassaService;
    private final SubscriptionService subscriptionService;

    public PaymentService(PaymentHistoryRepository paymentHistoryRepository,
                         YooKassaService yooKassaService,
                         SubscriptionService subscriptionService) {
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.yooKassaService = yooKassaService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Создать новый платеж
     */
    public PaymentHistory createPayment(UserAccount user, SubscriptionPlan subscriptionPlan, 
                                       PaymentHistory.PaymentMethod paymentMethod) {
        PaymentHistory payment = new PaymentHistory(
            user, 
            subscriptionPlan, 
            subscriptionPlan.getPrice(), 
            subscriptionPlan.getCurrency(), 
            paymentMethod
        );
        
        payment.setProviderName("YooKassa");
        return paymentHistoryRepository.save(payment);
    }

    /**
     * Инициировать платеж через ЮКассу
     */
    public PaymentInitiationResult initiatePayment(PaymentHistory payment) {
        try {
            UserAccount user = payment.getUserAccount();
            SubscriptionPlan plan = payment.getSubscriptionPlan();
            
            String description = "Подписка: " + plan.getDisplayName();
            
            YooKassaService.PaymentResult result = yooKassaService.createPayment(
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getCurrency(),
                description,
                user.getEmail()
            );

            if (result.isSuccess()) {
                payment.setProviderTransactionId(result.getPaymentId());
                payment.setPaymentUrl(result.getPaymentUrl());
                payment.setStatus(PaymentHistory.PaymentStatus.PROCESSING);
                payment.setWebhookData(result.getRawResponse());
                paymentHistoryRepository.save(payment);

                return new PaymentInitiationResult(
                    true,
                    payment.getTransactionId(),
                    result.getPaymentUrl(),
                    "Платеж инициирован успешно",
                    payment
                );
            } else {
                payment.markFailed(result.getMessage());
                paymentHistoryRepository.save(payment);

                return new PaymentInitiationResult(
                    false,
                    payment.getTransactionId(),
                    null,
                    result.getMessage(),
                    payment
                );
            }

        } catch (Exception e) {
            payment.markFailed("Ошибка инициации платежа: " + e.getMessage());
            paymentHistoryRepository.save(payment);

            return new PaymentInitiationResult(
                false,
                payment.getTransactionId(),
                null,
                e.getMessage(),
                payment
            );
        }
    }

    /**
     * Найти платеж по transactionId
     */
    public Optional<PaymentHistory> findByTransactionId(String transactionId) {
        return paymentHistoryRepository.findByTransactionId(transactionId);
    }
    
    /**
     * Найти платеж по transactionId (для контроллера)
     */
    public Optional<PaymentHistory> getPaymentByTransactionId(String transactionId) {
        return paymentHistoryRepository.findByTransactionId(transactionId);
    }

    /**
     * Обработать webhook от платежного провайдера
     */
    public void processWebhook(String transactionId, String status, String webhookData) {
        Optional<PaymentHistory> paymentOpt = paymentHistoryRepository.findByTransactionId(transactionId);
        
        if (paymentOpt.isEmpty()) {
            paymentOpt = paymentHistoryRepository.findByProviderTransactionId(transactionId);
        }
        
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Платеж не найден: " + transactionId);
        }

        PaymentHistory payment = paymentOpt.get();
        payment.setWebhookData(webhookData);

        // Мапинг статусов ЮКассы на наши внутренние статусы
        switch (status.toLowerCase()) {
            case "succeeded":
            case "completed":
            case "success":
                payment.markCompleted();
                paymentHistoryRepository.save(payment);
                // Активируем подписку после успешной оплаты
                subscriptionService.activateSubscription(payment);
                paymentHistoryRepository.save(payment); // Сохраняем связь с подпиской
                break;
            case "canceled":
            case "cancelled":
                payment.markCancelled();
                paymentHistoryRepository.save(payment);
                break;
            case "failed":
                payment.markFailed("Платеж не прошел");
                paymentHistoryRepository.save(payment);
                break;
            default:
                // Оставляем текущий статус для других статусов (pending, processing)
                paymentHistoryRepository.save(payment);
        }
    }
    
    
    /**
     * Получить историю платежей пользователя
     */
    public List<PaymentHistory> getUserPayments(UserAccount user) {
        return paymentHistoryRepository.findByUserAccountOrderByCreatedAtDesc(user);
    }

    /**
     * Результат инициации платежа
     */
    public static class PaymentInitiationResult {
        private final boolean success;
        private final String transactionId;
        private final String paymentUrl;
        private final String message;
        private final PaymentHistory payment;

        public PaymentInitiationResult(boolean success, String transactionId, String paymentUrl,
                                     String message, PaymentHistory payment) {
            this.success = success;
            this.transactionId = transactionId;
            this.paymentUrl = paymentUrl;
            this.message = message;
            this.payment = payment;
        }

        public boolean isSuccess() { return success; }
        public String getTransactionId() { return transactionId; }
        public String getPaymentUrl() { return paymentUrl; }
        public String getMessage() { return message; }
        public PaymentHistory getPayment() { return payment; }
    }
}

