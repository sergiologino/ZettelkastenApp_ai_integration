package com.example.integration.config;

import com.example.integration.service.ClientManagementService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * После миграций БД: у всех клиентских приложений без записи в {@code user_client_links} появляется владелец —
 * тот же пользователь, что и основной админ из {@code admin_users} (типичный деплой с одной парой admin/admin).
 */
@Component
@Order(100)
public class ClientOwnerLinkBootstrap implements ApplicationRunner {

    private final ClientManagementService clientManagementService;

    public ClientOwnerLinkBootstrap(ClientManagementService clientManagementService) {
        this.clientManagementService = clientManagementService;
    }

    @Override
    public void run(ApplicationArguments args) {
        clientManagementService.backfillMissingClientOwnerLinks();
    }
}
