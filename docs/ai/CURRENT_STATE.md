# Current State

## Реализовано

- Backend: Spring Boot, пакет `com.example.integration`.
- Аутентификация: JWT (AuthController, JwtAuthFilter), API Key (UserApiKeyController, ApiKeyAuthFilter).
- AI: AiController, AiOrchestrationService, NeuralClientFactory; клиенты OpenAI, Pollinations, Claude, Mistral, GigaChat, Whisper, Qwen, DeepSeek, YandexGpt; поддержка текстовых и image/video сетей (миграции V014, V015).
- Каталог нейросетей (V016): добавлены GPT-5.4/5.4 Pro, GPT-4.1/Mini/Nano, o3/o3-pro/o4-mini, Claude Opus 4.6/Sonnet 4.6/Opus 4.5/Sonnet 4.5/Haiku 4.5, Gemini 2.5 Pro/Flash, Grok 3/3 Mini, DeepSeek R1, Mistral Large 3. Все is_active=false до подключения API-ключей.
- NeuralClientFactory: провайдеры "google" и "xai" маршрутизируются через OpenAiClient (OpenAI-совместимый API).
- Клиенты приложений: UserClientController, ClientManagementService, UserClientService; привязка пользователей к клиентам.
- Сети (NeuralNetwork): CRUD через админку/сервисы, NetworkManagementService, NetworkAccessService; доступ клиентов к сетям с приоритетами.
- Подписки и оплата: SubscriptionController, SubscriptionService, SubscriptionLimitService; YooKassa (PaymentWebhookController, YooKassaService, PaymentService).
- Админка: AdminController, статистика и управление.
- OAuth: OAuthCallbackController, UserAuthController, OAuthService.
- БД: Flyway-миграции, сущности UserAccount, NeuralNetwork, ClientNetworkAccess, UserApiKey, Subscription, PaymentHistory и др.
- Конфигурация: .env-template, application.yml; деплой через docker-compose (см. DEPLOYMENT_GUIDE.md).

## Не в фокусе

- Фронтенд не развивается по текущему контракту.
- Доменная память (docs/ai/domains/) пока не заводилась.

## Следующие шаги

- Определяются по задачам; после каждой задачи этот файл обновляется.
