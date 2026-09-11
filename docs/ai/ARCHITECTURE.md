# Architecture

## Компоненты

- **Backend**: Spring Boot (Java). Порт по умолчанию: 8091.
- **БД**: PostgreSQL (порт 5433 в docker-compose).
- **Фронтенд**: есть (Vite), в текущем фокусе не участвует.

## Слои backend

| Слой | Назначение |
|------|------------|
| **Controller** | REST API: Auth, Ai, Admin, UserClient, UserApiKey, Subscription, NetworkAccess, Payment (webhook), OAuth callback |
| **Service** | Бизнес-логика: Auth, User, AiOrchestration, ClientManagement, NetworkManagement/NetworkAccess, Subscription, Payment (YooKassa), UserApiKey, RateLimit, SubscriptionLimit |
| **Client** | Внешние AI: NeuralClientFactory, OpenAiClient, YandexGptClient, ClaudeClient, MistralClient, GigaChatClient, WhisperClient, QwenClient, DeepSeekClient, VirtualTryOnClient, FashnClient, KlingVirtualTryOnClient, SeasonHitVideoClient (BaseNeuralClient) |
| **Security** | JwtAuthFilter, ApiKeyAuthFilter, SecurityConfig, EncryptionService |

## Потоки

1. **AI-запрос**: приложение → API (JWT или API Key) → AiController → AiOrchestrationService → NeuralClientFactory → провайдер (OpenAI, Yandex, Anthropic, xAI, FASHN, Kling и т.д.).
2. **Управление**: админ/пользователь → соответствующие контроллеры → сервисы → репозитории → PostgreSQL.
3. **Оплата**: YooKassa webhook → PaymentWebhookController → PaymentService/YooKassaService.

## Внешние зависимости

- AI-провайдеры (OpenAI API, Yandex Cloud, Anthropic, xAI, FASHN, Kling и др.).
- YooKassa для подписок/оплаты.
- OAuth (при необходимости) для входа пользователей.
