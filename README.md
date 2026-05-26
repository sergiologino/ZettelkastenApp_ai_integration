# AI Integration Service

Универсальный сервис для интеграции с различными нейросетями (OpenAI GPT-4, Yandex GPT, Claude, Mistral, GigaChat, Whisper).

## 📚 Документация по деплою

### Быстрый старт:
- **[🚀 QUICK_START.md](QUICK_START.md)** - Быстрый запуск backend (3 минуты)
- **[⚡ TIMEWEB_QUICK_REFERENCE.md](TIMEWEB_QUICK_REFERENCE.md)** - Краткая памятка по всем командам

### Деплой на Timeweb:
1. **[⚡ TIMEWEB (специальная версия)](TIMEWEB_DEPLOY.md)** - ⭐ **ДЛЯ TIMEWEB** - Без volumes, с автобэкапом
2. **[🐳 Без Nginx (Docker only)](DOCKER_ONLY_DEPLOY.md)** - Для других хостингов с Docker
3. **[🔧 С Nginx (полный)](DEPLOYMENT_GUIDE.md)** - Полное руководство с Nginx
4. **[🎨 Frontend отдельно](FRONTEND_DEPLOY_GUIDE.md)** - Если нужно деплоить фронт отдельно
5. **[🔌 Интеграция с noteapp](NOTEAPP_INTEGRATION_GUIDE.md)** - Настройка noteapp для работы с AI Integration

### Troubleshooting:
- **[🔧 FIX_CONNECTION_ERROR.md](FIX_CONNECTION_ERROR.md)** - Исправление "Connection to localhost:5432 refused"

---

## 🚀 Возможности

- **Мульти-провайдерная поддержка**: OpenAI, Yandex, Anthropic Claude, Mistral, GigaChat, Whisper
- **Управление лимитами**: Настраиваемые лимиты для разных типов пользователей
- **Автоматическое переключение**: Fallback на бесплатные нейросети при исчерпании лимита
- **Логирование запросов**: Полное логирование всех запросов и ответов
- **Аутентификация**: API-ключи для клиентских приложений, JWT для администраторов
- **REST API**: Полноценный REST API с Swagger документацией
- **Docker поддержка**: Готовые Dockerfile и docker-compose

## 📋 Требования

- Java 17+
- PostgreSQL 16+
- Docker (опционально)

## 🛠️ Установка и запуск

### Вариант 1: Docker Compose (рекомендуется)

1. Скопируйте `.env.example` в `.env` и настройте переменные окружения:
```bash
cp .env.example .env
```

2. Запустите сервис:
```bash
docker-compose up -d
```

3. Проверьте статус:
```bash
docker-compose ps
```

Сервис будет доступен по адресу: `http://localhost:8091`

### Вариант 2: Локальный запуск

1. Создайте базу данных PostgreSQL:
```sql
CREATE DATABASE ai_integration_db;
```

2. Настройте переменные окружения:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/ai_integration_db
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your-secret-key-min-256-bits
```

3. Запустите приложение:
```bash
./gradlew bootRun
```

## 📚 API Документация

После запуска Swagger UI доступен по адресу:
```
http://localhost:8091/swagger-ui.html
```

### Основные эндпоинты:

#### Для клиентских приложений (требуется X-API-Key):
- `POST /api/ai/process` - Отправить запрос в AI

#### Для администраторов (требуется Bearer token):
- `GET /api/admin/networks` - Список нейросетей
- `POST /api/admin/networks` - Добавить нейросеть
- `GET /api/admin/clients` - Список клиентов
- `POST /api/admin/clients` - Добавить клиента
- `GET /api/admin/logs` - Логи запросов

#### Аутентификация:
- `POST /api/auth/login` - Вход администратора
- `POST /api/auth/register` - Регистрация первого администратора

## 🔑 Первичная настройка

### 1. Регистрация администратора

После первого запуска зарегистрируйте администратора:

```bash
curl -X POST http://localhost:8091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "your_secure_password"
  }'
```

Сохраните полученный JWT токен.

### 2. Добавление клиентского приложения

```bash
curl -X POST http://localhost:8091/api/admin/clients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "noteapp",
    "description": "AltaNote application"
  }'
```

Сохраните полученный API ключ.

### 3. Настройка нейросетей

Обновите API ключи для нужных нейросетей через Swagger UI или API:

```bash
curl -X PUT http://localhost:8091/api/admin/networks/{network_id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "openai-gpt4",
    "displayName": "OpenAI GPT-4",
    "provider": "openai",
    "networkType": "chat",
    "apiUrl": "https://api.openai.com/v1",
    "apiKey": "sk-your-openai-api-key",
    "modelName": "gpt-4",
    "isActive": true,
    "isFree": false,
    "priority": 10,
    "timeoutSeconds": 60,
    "maxRetries": 3
  }'
```

## 📊 Использование из клиентского приложения

### Пример запроса:

```bash
curl -X POST http://localhost:8091/api/ai/process \
  -H "Content-Type: application/json" \
  -H "X-API-Key: aikey_your_client_api_key" \
  -d '{
    "userId": "user123",
    "networkName": "openai-gpt4",
    "requestType": "chat",
    "payload": {
      "messages": [
        {
          "role": "user",
          "content": "Hello, how are you?"
        }
      ]
    }
  }'
```

### Пример ответа:

```json
{
  "requestId": "uuid",
  "status": "success",
  "networkUsed": "openai-gpt4",
  "response": {
    "choices": [
      {
        "message": {
          "role": "assistant",
          "content": "Hello! I'm doing well, thank you for asking..."
        }
      }
    ]
  },
  "executionTimeMs": 1250,
  "tokensUsed": 45,
  "usageLimitInfo": {
    "used": 45,
    "remaining": 955,
    "period": "daily"
  }
}
```

## 🔧 Конфигурация

### Переменные окружения:

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `DB_URL` | URL базы данных | `jdbc:postgresql://localhost:5432/ai_integration_db` |
| `DB_USERNAME` | Пользователь БД | `postgres` |
| `DB_PASSWORD` | Пароль БД | `postgres` |
| `SERVER_PORT` | Порт сервиса | `8091` |
| `JWT_SECRET` | Секретный ключ для JWT | *обязательно изменить в продакшене* |
| `JWT_EXPIRATION` | Время жизни JWT (мс) | `86400000` (24 часа) |
| `AI_REQUEST_TIMEOUT` | Таймаут запросов к AI (сек) | `60` |
| `AI_MAX_RETRIES` | Максимум повторных попыток | `3` |
| `AI_ENABLE_FALLBACK` | Автопереключение на free сети | `true` |

### Лимиты запросов:

По умолчанию настроены следующие лимиты:

- **Новые пользователи**: 10 запросов/день
- **Бесплатные пользователи**: 20-50 запросов/день (зависит от сети)
- **Платные пользователи**: 100 запросов/месяц (или безлимит на free сетях)

Лимиты настраиваются в админ-панели для каждой нейросети отдельно.

## 🗂️ Структура проекта

```
src/
├── main/
│   ├── java/com/example/integration/
│   │   ├── client/          # Клиенты нейросетей
│   │   ├── config/          # Конфигурация
│   │   ├── controller/      # REST контроллеры
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── model/           # Entity модели
│   │   ├── repository/      # JPA репозитории
│   │   ├── security/        # Security и JWT
│   │   └── service/         # Бизнес-логика
│   └── resources/
│       ├── db/migration/    # Flyway миграции
│       └── application.yml  # Конфигурация
```

## 🧪 Тестирование

```bash
./gradlew test
```

## 📝 Логи

### Консоль (`[AI-TRAFFIC]`)

Каждый `POST /api/ai/process` пишет в лог **тело запроса и ответа** (base64 заменён на `[base64 omitted, chars=N]`):

```
[AI-TRAFFIC] IN requestId=... client=... network=wibestyle-vton type=image_generation payload={...}
[AI-TRAFFIC] VTON route network=wibestyle-vton ... provider=virtual_try_on_grok
[AI-TRAFFIC] OUT requestId=... status=success response={...}
```

Уровень: `LOG_LEVEL_AI_TRAFFIC=INFO` (logger `AI_TRAFFIC`).

### БД и админка

Логи запросов хранятся в `request_logs` (тоже без base64) и доступны:

- UI: раздел **Логи запросов** (React admin)
- API:

```bash
curl -X GET "http://localhost:8091/api/admin/logs?page=0&size=50" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Детали одного запроса: `GET /api/admin/logs/{id}`.

## 🔒 Безопасность

- **API-ключи**: Храните API-ключи нейросетей в безопасном месте
- **JWT**: Используйте сильный секретный ключ (минимум 256 бит)
- **HTTPS**: В продакшене используйте только HTTPS
- **Rate Limiting**: Настройте лимиты для предотвращения злоупотреблений

## 📞 Поддержка

При возникновении проблем:
1. Проверьте логи: `docker-compose logs -f ai-service`
2. Проверьте подключение к БД
3. Убедитесь, что все переменные окружения настроены

## 🎯 Roadmap

- [ ] Веб-интерфейс администратора (React + Tailwind)
- [ ] Поддержка streaming ответов
- [ ] Кэширование частых запросов
- [ ] Метрики и мониторинг (Prometheus, Grafana)
- [ ] Шифрование API ключей в БД
- [ ] Rate limiting на уровне IP адресов

## 📄 Лицензия

Proprietary - All rights reserved

