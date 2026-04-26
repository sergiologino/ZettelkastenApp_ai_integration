# Текущее состояние (AI Integration Service)

## Репозиторий

- **Проект**: Spring Boot **3.4**, Java **17**, PostgreSQL, Flyway, Docker (`docker-compose.yml`).
- **Назначение**: единая точка доступа внешних приложений к нескольким провайдерам нейросетей (OpenAI, Yandex, Anthropic, Mistral, GigaChat, Whisper и др.) с учётом лимитов, логов и админ-управления.
- **Память проекта для агентов**: каталог **`docs/ai/*`** (этот файл, `CHANGELOG_AI.md`, `EXTERNAL_SERVICES_INTEGRATION.md`, PROJECT_OVERVIEW, ARCHITECTURE, DECISIONS, CONVENTIONS).
- **Корневая документация**: `README.md`, деплой-гайды (`QUICK_START.md`, `TIMEWEB_DEPLOY.md`, и т.д.).

## Архитектура подключения внешних сервисов

1. **Администратор** (JWT): настраивает нейросети (`/api/admin/networks`), клиентские приложения (`/api/admin/clients`), **доступ клиентов к сетям** (`/api/admin/access/**`).
2. **Клиентское приложение** (заголовок **`X-API-Key`**): вызывает **`POST /api/ai/process`** и вспомогательные **`GET /api/ai/networks/**`**; ключ выдаётся при создании клиента (префикс `aikey_`).
3. Опционально: **пользовательский кабинет** (`/api/user/**`) — OAuth, подписки, сохранение собственных API-ключей провайдеров; для минимальной интеграции «сервис → нейросеть» достаточно п. 1–2.

## Реализовано

- Backend: пакет `com.example.integration`. Аутентификация: JWT (`AuthController`, `JwtAuthFilter`), API Key (`ApiKeyAuthFilter`), пользовательские ключи (`UserApiKeyController`).
- **AI**: `AiController`, `AiOrchestrationService`, `NeuralClientFactory`; клиенты OpenAI, Pollinations, Claude, Mistral, GigaChat, Whisper, Qwen, DeepSeek, YandexGpt; поддержка текстовых, транскрипции, image/video сетей (миграции V014, V015). Провайдеры **google** и **xai** маршрутизируются через `OpenAiClient` (OpenAI-совместимый API).
- **Каталог нейросетей (V016)**: GPT-5.4/5.4 Pro, GPT-4.1/Mini/Nano, o3/o3-pro/o4-mini, Claude Opus 4.6/Sonnet 4.6/Opus 4.5/Sonnet 4.5/Haiku 4.5, Gemini 2.5 Pro/Flash, Grok 3/3 Mini, DeepSeek R1, Mistral Large 3 (записи по умолчанию с `is_active=false` до подключения ключей).
- **Синтез речи (`speech_synthesis`)**: OpenAI TTS (`/audio/speech`), Yandex SpeechKit TTS (`tts:synthesize`, тот же API-ключ, что у Yandex GPT).
- **Назначение сетей клиентам**: таблица `client_network_access`, API `/api/admin/access` (в т.ч. `POST .../grant-all/{clientId}`).
- Клиенты приложений: `UserClientController`, `ClientManagementService`, `UserClientService`; привязка пользователей к клиентам.
- Подписки и оплата: `SubscriptionController`, YooKassa (`PaymentWebhookController`, и др.).
- БД: Flyway; сущности `UserAccount`, `NeuralNetwork`, `ClientNetworkAccess`, `UserApiKey`, подписки, платежи и др.
- Админ- и AI-API в OpenAPI; Swagger UI: `/swagger-ui/**`, спецификация: `/v3/api-docs`. Actuator: `/actuator/health`, `/actuator/prometheus`.
- **Dockerfile**: runtime-образ `eclipse-temurin:17-jdk-jammy` (заменён устаревший `openjdk:17-jdk-slim`).
- **Social posting API**: клиентский endpoint `POST /api/social/posts` под `X-API-Key` публикует текстовые посты в Telegram, Facebook и X. Ключи платформ передаются в теле запроса и не сохраняются; результаты логируются в `request_logs` с `request_type = social_post:<platform>`. Админская статистика: `GET /api/admin/social/stats`; во фронте добавлен только блок статистики, без настроек ключей.

## План и бэклог (из README и кода)

- Веб-админка (React-фронт в репозитории `frontend/`), streaming-ответы, кэш, расширенный мониторинг.
- Доработки лимитов/метрик в ответах (часть полей помечена TODO в коде).
- Настройки social-платформ в админке/кабинете пока не реализованы: по текущему контракту секреты передаёт клиент в каждом запросе.

## Не в фокусе

- Фронтенд не развивается по текущему контракту памяти.
- Доменная память (`docs/ai/domains/`) пока не заводилась.

## Следующие шаги (приоритет для интеграторов)

1. Завести клиента и выдать **`X-API-Key`**, выдать доступ к нужным нейросетям (`grant-all` или точечный `POST /api/admin/access`).
2. В вызывающем сервисе хранить **`BASE_URL`** и **`X-API-Key`** в переменных окружения.
3. Для контракта тел запросов/ответов опираться на **`docs/ai/EXTERNAL_SERVICES_INTEGRATION.md`** и OpenAPI.
