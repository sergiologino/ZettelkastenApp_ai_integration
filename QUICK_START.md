# Быстрый старт AI Integration Service

## Проблема: Connection to localhost:5432 refused ❌

Если вы видите эту ошибку - **переменные окружения не передаются в контейнер!**

## Быстрое решение ✅

### Шаг 1: Создайте .env файл на сервере

```bash
cd /path/to/noteapp-ai-integration
nano .env
```

Вставьте следующее:

```env
# Database Configuration
DB_NAME=ai_integration_db
DB_USER=postgres
DB_PASSWORD=postgres
DB_PORT=5433

# JWT Configuration (ОБЯЗАТЕЛЬНО измените на проде!)
JWT_SECRET=your-super-secret-jwt-key-min-256-bits-for-production-change-this
JWT_EXPIRATION=86400000

# Encryption Configuration (ОБЯЗАТЕЛЬНО измените! Должен быть ровно 32 символа)
ENCRYPTION_SECRET_KEY=change-this-to-32-byte-secret

# AI Service Configuration
AI_REQUEST_TIMEOUT=60
AI_MAX_RETRIES=3
AI_ENABLE_FALLBACK=true

# Server Configuration
SERVICE_PORT=8091

# Logging
LOG_LEVEL=INFO
SHOW_SQL=false
```

**Сохраните файл** (Ctrl+O, Enter, Ctrl+X в nano)

### Шаг 2: Остановите старые контейнеры

```bash
docker-compose down
```

### Шаг 3: Запустите с новой конфигурацией

```bash
docker-compose up -d
```

### Шаг 4: Проверьте логи

```bash
# Смотрим логи приложения
docker logs ai-integration-service -f
```

**Должно быть:**
```
Started NoteappAiIntegrationApplication in X seconds
Tomcat started on port 8091
```

**НЕ должно быть:**
```
Connection to localhost:5432 refused  ❌
```

### Шаг 5: Проверьте health

```bash
curl http://localhost:8091/actuator/health
```

**Ожидаемый ответ:**
```json
{"status":"UP"}
```

---

## Проверка переменных окружения

Если все еще не работает, проверьте, что переменные передаются:

```bash
docker exec ai-integration-service printenv | grep DB_URL
```

**Должно быть:**
```
DB_URL=jdbc:postgresql://postgres:5432/ai_integration_db
```

**НЕ должно быть:**
```
DB_URL=jdbc:postgresql://localhost:5432/ai_integration_db  ❌
```

---

## Генерация безопасных ключей для ПРОДАКШЕНА

**ВАЖНО:** На продакшене НЕ используйте дефолтные значения!

```bash
# Сгенерируйте JWT Secret (64 символа base64)
openssl rand -base64 64

# Сгенерируйте Encryption Key (32 символа)
openssl rand -hex 16
```

Замените значения в `.env` файле.

---

## Что делать дальше

- ✅ Читайте [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) для полной инструкции
- ✅ Настройте Nginx для SSL/HTTPS
- ✅ Настройте firewall
- ✅ Сделайте backup базы данных

---

## Если проблема не решена

### Диагностика

```bash
# 1. Проверьте статус контейнеров
docker-compose ps

# 2. Проверьте сеть
docker network inspect ai-integration-network

# 3. Проверьте логи БД
docker logs ai-integration-db

# 4. Проверьте конфигурацию
docker-compose config
```

### Полная перезагрузка

```bash
# Удалите все контейнеры и volumes
docker-compose down -v

# Пересоберите образы
docker-compose build --no-cache

# Запустите снова
docker-compose up -d

# Смотрите логи
docker-compose logs -f
```

