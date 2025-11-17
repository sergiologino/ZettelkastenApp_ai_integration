package com.example.integration.service;

import com.example.integration.model.Subscription;
import com.example.integration.model.SubscriptionPlan;
import com.example.integration.model.UserAccount;
import com.example.integration.repository.SubscriptionPlanRepository;
import com.example.integration.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для управления подписками пользователей
 */
@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                              SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    /**
     * Получить все доступные тарифные планы
     */
    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findByIsActiveTrueOrderBySortOrder();
    }

    /**
     * Получить план по имени
     */
    public Optional<SubscriptionPlan> getPlanByName(String name) {
        return subscriptionPlanRepository.findByName(name);
    }

    /**
     * Получить бесплатный план
     */
    public SubscriptionPlan getFreePlan() {
        return subscriptionPlanRepository.findFirstByPriceAndIsActiveTrueOrderBySortOrder(java.math.BigDecimal.ZERO)
                .orElseThrow(() -> new RuntimeException("Бесплатный план не найден"));
    }

    /**
     * Получить текущую подписку пользователя
     */
    public Optional<Subscription> getCurrentSubscription(UserAccount user) {
        return subscriptionRepository.findCurrentActiveSubscription(user, LocalDateTime.now());
    }

    /**
     * Создать новую подписку для пользователя
     */
    public Subscription createSubscription(UserAccount user, SubscriptionPlan plan) {
        // Отменяем текущую активную подписку, если есть
        getCurrentSubscription(user).ifPresent(currentSub -> {
            currentSub.cancel("Переход на новый план");
            subscriptionRepository.save(currentSub);
        });

        // Создаем новую подписку
        Subscription subscription = new Subscription(user, plan);
        subscription = subscriptionRepository.save(subscription);

        // Обновляем статус пользователя
        updateUserSubscriptionStatus(user, subscription);

        return subscription;
    }

    /**
     * Обновить статус подписки пользователя в модели UserAccount
     */
    public void updateUserSubscriptionStatus(UserAccount user, Subscription subscription) {
        if (subscription.isActive()) {
            if (subscription.getSubscriptionPlan().isFree()) {
                user.setSubscriptionStatus(UserAccount.SubscriptionStatus.FREE);
            } else {
                user.setSubscriptionStatus(UserAccount.SubscriptionStatus.ACTIVE);
            }
            if (subscription.getExpiresAt() != null) {
                user.setSubscriptionExpiresAt(java.sql.Timestamp.valueOf(subscription.getExpiresAt()));
            }
        } else if (subscription.isExpired()) {
            user.setSubscriptionStatus(UserAccount.SubscriptionStatus.EXPIRED);
        } else if (subscription.getStatus() == Subscription.SubscriptionStatus.CANCELLED) {
            user.setSubscriptionStatus(UserAccount.SubscriptionStatus.CANCELLED);
        }
    }

    /**
     * Отменить подписку
     */
    public void cancelSubscription(UserAccount user, String reason) {
        getCurrentSubscription(user).ifPresent(subscription -> {
            subscription.cancel(reason);
            subscriptionRepository.save(subscription);
            updateUserSubscriptionStatus(user, subscription);
        });
    }

    /**
     * Получить историю подписок пользователя
     */
    public List<Subscription> getUserSubscriptionHistory(UserAccount user) {
        return subscriptionRepository.findByUserAccountOrderByCreatedAtDesc(user);
    }
    
    /**
     * Активировать подписку после успешной оплаты
     */
    public void activateSubscription(com.example.integration.model.PaymentHistory payment) {
        UserAccount user = payment.getUserAccount();
        SubscriptionPlan plan = payment.getSubscriptionPlan();

        // Создаем или обновляем подписку
        Subscription subscription;
        if (payment.getSubscription() != null) {
            subscription = payment.getSubscription();
            subscription.renew();
        } else {
            subscription = createSubscription(user, plan);
            payment.setSubscription(subscription);
        }

        subscription = subscriptionRepository.save(subscription);
        updateUserSubscriptionStatus(user, subscription);
        
        // Сохраняем связь payment с subscription (нужно сохранить через PaymentHistoryRepository)
        // Это будет сделано в PaymentService после вызова этого метода
    }
}

