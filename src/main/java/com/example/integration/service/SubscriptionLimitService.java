package com.example.integration.service;

import com.example.integration.model.*;
import com.example.integration.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для проверки лимитов подписки при запросах к нейросетям
 */
@Service
@Transactional
public class SubscriptionLimitService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionLimitService.class);
    
    private final UserClientLinkRepository userClientLinkRepository;
    private final SubscriptionService subscriptionService;
    private final ClientNetworkAccessRepository clientNetworkAccessRepository;
    private final RequestLogRepository requestLogRepository;

    public SubscriptionLimitService(
            UserClientLinkRepository userClientLinkRepository,
            SubscriptionService subscriptionService,
            ClientNetworkAccessRepository clientNetworkAccessRepository,
            RequestLogRepository requestLogRepository) {
        this.userClientLinkRepository = userClientLinkRepository;
        this.subscriptionService = subscriptionService;
        this.clientNetworkAccessRepository = clientNetworkAccessRepository;
        this.requestLogRepository = requestLogRepository;
    }

    /**
     * Проверить, можно ли выполнить запрос к нейросети
     * @return null если запрос разрешен, или сообщение об ошибке
     */
    public String checkRequestLimit(ClientApplication clientApp, NeuralNetwork network) {
        log.info("🔍 [SubscriptionLimitService] ===== Проверка лимитов для запроса =====");
        log.info("   Клиент: {} (ID: {})", clientApp.getName(), clientApp.getId());
        log.info("   Нейросеть: {} (ID: {}, name: {})", network.getDisplayName(), network.getId(), network.getName());
        
        // 1. Получить владельца клиентского приложения
        Optional<UserClientLink> linkOpt = userClientLinkRepository.findByClientApplication(clientApp.getId());
        if (linkOpt.isEmpty()) {
            log.warn("⚠️ [SubscriptionLimitService] Клиент {} не привязан к пользователю", clientApp.getName());
            return "Клиентское приложение не привязано к пользователю";
        }

        UserAccount user = linkOpt.get().getUser();
        log.info("   Пользователь: {} (ID: {})", user.getEmail(), user.getId());

        // 2. Получить текущую подписку
        Optional<Subscription> subscriptionOpt = subscriptionService.getCurrentSubscription(user);
        SubscriptionPlan plan;
        boolean isFreePlan;

        if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().isActive()) {
            // Нет активной подписки - используем бесплатный план
            plan = subscriptionService.getFreePlan();
            isFreePlan = true;
            log.info("   План: бесплатный (нет активной подписки)");
        } else {
            plan = subscriptionOpt.get().getSubscriptionPlan();
            isFreePlan = plan.isFree();
            log.info("   План: {} (бесплатный: {})", plan.getName(), isFreePlan);
        }

        // 3. Получить доступ к нейросети для этого клиента
        log.info("   🔍 Ищем доступ к нейросети для клиента {} и нейросети {}", clientApp.getId(), network.getId());
        Optional<ClientNetworkAccess> accessOpt = clientNetworkAccessRepository
                .findByClientApplicationAndNeuralNetwork(clientApp, network);

        if (accessOpt.isEmpty()) {
            log.error("❌ [SubscriptionLimitService] Доступ не найден! Клиент: {}, Нейросеть: {} ({})", 
                clientApp.getName(), network.getName(), network.getId());
            
            // Дополнительная диагностика: проверим все доступы этого клиента
            List<ClientNetworkAccess> allAccesses = clientNetworkAccessRepository
                .findByClientApplicationOrderByNeuralNetworkDisplayNameAsc(clientApp);
            log.info("   📋 Всего доступов для клиента {}: {}", clientApp.getName(), allAccesses.size());
            allAccesses.forEach(access -> {
                log.info("      - Нейросеть: {} (ID: {}, name: {})", 
                    access.getNeuralNetwork().getDisplayName(), 
                    access.getNeuralNetwork().getId(),
                    access.getNeuralNetwork().getName());
            });
            
            return "Нейросеть не подключена к вашему клиентскому приложению";
        }
        
        log.info("✅ [SubscriptionLimitService] Доступ найден для нейросети {}", network.getName());

        ClientNetworkAccess access = accessOpt.get();

        // 4. Проверка лимитов в зависимости от плана
        if (isFreePlan) {
            // Бесплатный план: проверяем free_request_limit
            if (access.getFreeRequestLimit() != null && access.getFreeRequestLimit() > 0) {
                // Подсчитываем использованные бесплатные запросы
                Long usedFreeRequests = requestLogRepository.countByClientAndNetworkAndFreePlan(
                        clientApp.getId(), network.getId());
                
                if (usedFreeRequests != null && usedFreeRequests >= access.getFreeRequestLimit()) {
                    return String.format(
                            "Достигнут лимит бесплатных запросов (%d из %d). " +
                            "Для увеличения лимита приобретите платную подписку.",
                            usedFreeRequests, access.getFreeRequestLimit()
                    );
                }
            } else {
                // Если free_request_limit не установлен, разрешаем только бесплатные нейросети
                // Проверяем, является ли нейросеть бесплатной (можно добавить поле isFree в NeuralNetwork)
                // Пока разрешаем все запросы, если free_request_limit не установлен
            }
        } else {
            // Платный план: проверяем daily_request_limit и monthly_request_limit
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

            // Проверка дневного лимита
            if (access.getDailyRequestLimit() != null && access.getDailyRequestLimit() > 0) {
                Long dailyRequests = requestLogRepository.countByClientAndNetworkAndDateRange(
                        clientApp.getId(), network.getId(), startOfDay, LocalDateTime.now());
                
                if (dailyRequests != null && dailyRequests >= access.getDailyRequestLimit()) {
                    return String.format(
                            "Достигнут дневной лимит запросов (%d из %d). " +
                            "Лимит обновится завтра.",
                            dailyRequests, access.getDailyRequestLimit()
                    );
                }
            }

            // Проверка месячного лимита
            if (access.getMonthlyRequestLimit() != null && access.getMonthlyRequestLimit() > 0) {
                Long monthlyRequests = requestLogRepository.countByClientAndNetworkAndDateRange(
                        clientApp.getId(), network.getId(), startOfMonth, LocalDateTime.now());
                
                if (monthlyRequests != null && monthlyRequests >= access.getMonthlyRequestLimit()) {
                    return String.format(
                            "Достигнут месячный лимит запросов (%d из %d). " +
                            "Лимит обновится в начале следующего месяца.",
                            monthlyRequests, access.getMonthlyRequestLimit()
                    );
                }
            }

            // Если лимиты не установлены, разрешаем запрос (без ограничений до лимита нейросети)
        }

        return null; // Запрос разрешен
    }
}

