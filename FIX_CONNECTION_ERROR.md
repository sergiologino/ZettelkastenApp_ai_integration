# Исправление ошибки: Connection to localhost:5432 refused

## Проблема

```
Connection to localhost:5432 refused. Check that the hostname and port are correct 
and that the postmaster is accepting TCP/IP connections.
```

## Причина

Приложение **не получает переменные окружения** при деплое и использует дефолтное значение `localhost:5432` из `application.yml`:

```yaml
datasource:
  url: ${DB_URL:jdbc:postgresql://localhost:5432/ai_integration_db}  # ❌ localhost по умолчанию!
```

В Docker контейнере `localhost` указывает на **сам контейнер**, а не на хост или другой контейнер PostgreSQL.

## Решение

### ✅ Исправления в коде

1. **Добавлена переменная `ENCRYPTION_SECRET_KEY` в `docker-compose.yml`**
   - Строка 46-47: добавлен `ENCRYPTION_SECRET_KEY` в `environment`

### ✅ Документация

Созданы файлы:
- **`.env-template`** - шаблон с переменными окружения
- **`QUICK_START.md`** - быстрая инструкция по запуску
- **`DEPLOYMENT_GUIDE.md`** - полное руководство по деплою

---

## Как запустить на сервере

### Вариант 1: Копирование .env-template

```bash
# На локальной машине
scp .env-template user@server:/path/to/project/.env

# На сервере
cd /path/to/project
nano .env  # Измените пароли и секреты!
docker-compose down
docker-compose up -d
```

### Вариант 2: Создание .env вручную

```bash
# На сервере
cd /path/to/project
nano .env
```

Вставьте содержимое:

```env
DB_NAME=ai_integration_db
DB_USER=postgres
DB_PASSWORD=postgres
DB_PORT=5433

JWT_SECRET=your-super-secret-jwt-key-min-256-bits-for-production-change-this
JWT_EXPIRATION=86400000

ENCRYPTION_SECRET_KEY=change-this-to-32-byte-secret

AI_REQUEST_TIMEOUT=60
AI_MAX_RETRIES=3
AI_ENABLE_FALLBACK=true

SERVICE_PORT=8091

LOG_LEVEL=INFO
SHOW_SQL=false
```

Сохраните и запустите:

```bash
docker-compose down
docker-compose up -d
docker logs ai-integration-service -f
```

---

## Проверка решения

### 1. Логи должны показывать успешное подключение

```bash
docker logs ai-integration-service -f
```

**✅ Правильно:**
```
HikariPool-1 - Start completed.
Flyway migration completed successfully
Started NoteappAiIntegrationApplication in 12.5 seconds
```

**❌ Неправильно:**
```
Connection to localhost:5432 refused
```

### 2. Проверка переменных окружения

```bash
docker exec ai-integration-service printenv | grep DB_URL
```

**✅ Правильно:**
```
DB_URL=jdbc:postgresql://postgres:5432/ai_integration_db
```

**❌ Неправильно:**
```
DB_URL=jdbc:postgresql://localhost:5432/ai_integration_db
```

### 3. Health check

```bash
curl http://localhost:8091/actuator/health
```

**✅ Ожидаемый ответ:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

---

## Если проблема сохраняется

### Диагностика сети

```bash
# 1. Проверьте, что оба контейнера в одной сети
docker network inspect ai-integration-network

# Должны быть:
# - ai-integration-db (postgres)
# - ai-integration-service (ai-service)

# 2. Проверьте доступность postgres из контейнера приложения
docker exec -it ai-integration-service sh
ping postgres  # Должен резолвиться
exit
```

### Полная перезагрузка

```bash
# Удалите все, включая volumes
docker-compose down -v

# Удалите образы (опционально)
docker rmi $(docker images | grep ai-integration | awk '{print $3}')

# Пересоберите
docker-compose build --no-cache

# Запустите
docker-compose up -d

# Проверьте логи
docker-compose logs -f
```

### Проверка PostgreSQL

```bash
# Логи БД
docker logs ai-integration-db

# Должно быть:
# "database system is ready to accept connections"

# Подключение к БД напрямую (с хоста)
psql -h localhost -p 5433 -U postgres -d ai_integration_db
# Пароль: postgres (или ваш из .env)
```

---

## Безопасность для ПРОДАКШЕНА

⚠️ **КРИТИЧЕСКИ ВАЖНО**: На продакшене измените все дефолтные значения!

### Генерация безопасных ключей

```bash
# JWT Secret (минимум 256 бит)
JWT_SECRET=$(openssl rand -base64 64)
echo "JWT_SECRET=$JWT_SECRET"

# Encryption Key (ровно 32 символа)
ENCRYPTION_KEY=$(openssl rand -hex 16)
echo "ENCRYPTION_SECRET_KEY=$ENCRYPTION_KEY"

# Пароль БД
DB_PASSWORD=$(openssl rand -base64 24)
echo "DB_PASSWORD=$DB_PASSWORD"
```

Обновите `.env` файл с этими значениями.

---

## Измененные файлы

### docker-compose.yml
- **Строка 46-47**: Добавлен `ENCRYPTION_SECRET_KEY` в переменные окружения

### Новые файлы
- `.env-template` - шаблон переменных окружения
- `QUICK_START.md` - быстрый старт
- `DEPLOYMENT_GUIDE.md` - полное руководство
- `FIX_CONNECTION_ERROR.md` - этот файл

---

## Дальнейшие шаги

1. ✅ Запустите приложение с правильными переменными окружения
2. ✅ Проверьте health endpoint
3. ✅ Настройте Nginx reverse proxy (см. DEPLOYMENT_GUIDE.md)
4. ✅ Настройте SSL/HTTPS
5. ✅ Настройте firewall
6. ✅ Настройте backup БД
7. ✅ Настройте мониторинг (Prometheus + Grafana)

Читайте [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) для деталей.

