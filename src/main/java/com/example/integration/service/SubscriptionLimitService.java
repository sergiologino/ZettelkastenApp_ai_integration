package com.example.integration.service;

import com.example.integration.model.*;
import com.example.integration.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Сервис для проверки лимитов подписки при запросах к нейросетям
 */
@Service
@Transactional
public class SubscriptionLimitService {

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
        // 1. Получить владельца клиентского приложения
        Optional<UserClientLink> linkOpt = userClientLinkRepository.findByClientApplication(clientApp.getId());
        if (linkOpt.isEmpty()) {
            return "Клиентское приложение не привязано к пользователю";
        }

        UserAccount user = linkOpt.get().getUser();

        // 2. Получить текущую подписку
        Optional<Subscription> subscriptionOpt = subscriptionService.getCurrentSubscription(user);
        SubscriptionPlan plan;
        boolean isFreePlan;

        if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().isActive()) {
            // Нет активной подписки - используем бесплатный план
            plan = subscriptionService.getFreePlan();
            isFreePlan = true;
        } else {
            plan = subscriptionOpt.get().getSubscriptionPlan();
            isFreePlan = plan.isFree();
        }

        // 3. Получить доступ к нейросети для этого клиента
        Optional<ClientNetworkAccess> accessOpt = clientNetworkAccessRepository
                .findByClientApplicationAndNeuralNetwork(clientApp, network);

        if (accessOpt.isEmpty()) {
            return "Нейросеть не подключена к вашему клиентскому приложению";
        }

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

