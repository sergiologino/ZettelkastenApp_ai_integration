package com.example.integration.service;

import com.example.integration.model.*;
import com.example.integration.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления лимитами и проверки доступности
 */
@Service
public class RateLimitService {
    
    private final UsageCounterRepository usageCounterRepository;
    private final NetworkLimitRepository networkLimitRepository;
    private final NeuralNetworkRepository neuralNetworkRepository;
    
    public RateLimitService(
        UsageCounterRepository usageCounterRepository,
        NetworkLimitRepository networkLimitRepository,
        NeuralNetworkRepository neuralNetworkRepository
    ) {
        this.usageCounterRepository = usageCounterRepository;
        this.networkLimitRepository = networkLimitRepository;
        this.neuralNetworkRepository = neuralNetworkRepository;
    }
    
    /**
     * Проверить, доступна ли нейросеть для пользователя
     */
    @Transactional(readOnly = true)
    public boolean isNetworkAvailable(ExternalUser user, NeuralNetwork network) {
        // Получаем лимит для этого типа пользователя
        Optional<NetworkLimit> limit = networkLimitRepository
            .findByNeuralNetworkAndUserTypeAndLimitPeriod(network, user.getUserType(), "daily");
        
        if (limit.isEmpty() || limit.get().getRequestLimit() == null) {
            // Нет лимита = unlimited
            return true;
        }
        
        // Проверяем текущее использование
        LocalDate today = LocalDate.now();
        Optional<UsageCounter> counter = usageCounterRepository
            .findActiveCounterForUserAndNetwork(user, network, today);
        
        if (counter.isEmpty()) {
            return true; // Ещё не было запросов
        }
        
        return counter.get().getRequestCount() < limit.get().getRequestLimit();
    }
    
    /**
     * Получить оставшиеся запросы для пользователя и нейросети
     */
    @Transactional(readOnly = true)
    public Integer getRemainingRequests(ExternalUser user, NeuralNetwork network) {
        Optional<NetworkLimit> limit = networkLimitRepository
            .findByNeuralNetworkAndUserTypeAndLimitPeriod(network, user.getUserType(), "daily");
        
        if (limit.isEmpty() || limit.get().getRequestLimit() == null) {
            return null; // Unlimited
        }
        
        LocalDate today = LocalDate.now();
        Optional<UsageCounter> counter = usageCounterRepository
            .findActiveCounterForUserAndNetwork(user, network, today);
        
        int used = counter.map(UsageCounter::getRequestCount).orElse(0);
        return Math.max(0, limit.get().getRequestLimit() - used);
    }
    
    /**
     * Зарегистрировать использование
     */
    @Transactional
    public void recordUsage(ExternalUser user, NeuralNetwork network, Integer tokensUsed) {
        LocalDate today = LocalDate.now();
        
        Optional<UsageCounter> counterOpt = usageCounterRepository
            .findActiveCounterForUserAndNetwork(user, network, today);
        
        UsageCounter counter;
        if (counterOpt.isPresent()) {
            counter = counterOpt.get();
        } else {
            counter = new UsageCounter();
            counter.setExternalUser(user);
            counter.setNeuralNetwork(network);
            counter.setPeriodStart(today);
            counter.setPeriodEnd(today); // Дневной период
        }
        
        counter.incrementRequests(tokensUsed != null ? tokensUsed : 0);
        usageCounterRepository.save(counter);
    }
    
    /**
     * Найти доступную альтернативную нейросеть (бесплатную) при исчерпании лимита
     */
    @Transactional(readOnly = true)
    public Optional<NeuralNetwork> findFallbackNetwork(ExternalUser user, String networkType) {
        // Ищем бесплатные нейросети того же типа
        List<NeuralNetwork> freeNetworks = neuralNetworkRepository
            .findByTypeOrderedByPriority(networkType)
            .stream()
            .filter(n -> n.getIsFree() && n.getIsActive())
            .toList();
        
        for (NeuralNetwork network : freeNetworks) {
            if (isNetworkAvailable(user, network)) {
                return Optional.of(network);
            }
        }
        
        return Optional.empty();
    }
}

