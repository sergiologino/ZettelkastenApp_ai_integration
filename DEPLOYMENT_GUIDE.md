# AI Integration Service - Deployment Guide

## Проблема: Connection to localhost:5432 refused

Если вы видите эту ошибку при деплое:
```
Connection to localhost:5432 refused. Check that the hostname and port are correct
```

Это означает, что приложение не может подключиться к PostgreSQL.

## Причины

1. **Приложение запущено не через docker-compose** - переменные окружения не передаются
2. **Отсутствует .env файл** - используются дефолтные значения (localhost:5432)
3. **PostgreSQL контейнер не запущен** или недоступен

## Решение

### Вариант 1: Локальная разработка / тестирование

#### 1. Создайте .env файл

```bash
cp .env.example .env
```

#### 2. Отредактируйте .env (если нужно)

```env
# Database Configuration
DB_NAME=ai_integration_db
DB_USER=postgres
DB_PASSWORD=your_secure_password  # Измените!
DB_PORT=5433

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-min-256-bits  # Измените!
JWT_EXPIRATION=86400000

# Encryption Configuration
ENCRYPTION_SECRET_KEY=change-this-to-32-byte-key!!  # Измените! Должен быть 32 символа

# Server Configuration
SERVICE_PORT=8091
```

#### 3. Запустите через docker-compose

```bash
docker-compose up -d
```

#### 4. Проверьте логи

```bash
# Логи приложения
docker logs ai-integration-service -f

# Логи базы данных
docker logs ai-integration-db -f
```

#### 5. Проверьте здоровье сервиса

```bash
curl http://localhost:8091/actuator/health
```

Должен вернуть:
```json
{"status":"UP"}
```

### Вариант 2: Продакшн деплой (на сервере)

#### 1. Подготовьте переменные окружения на сервере

Создайте `.env` файл на сервере:

```bash
nano .env
```

Добавьте **безопасные** значения:

```env
# Database Configuration
DB_NAME=ai_integration_db
DB_USER=ai_user
DB_PASSWORD=STRONG_PASSWORD_HERE  # Генерируйте сильный пароль!
DB_PORT=5433

# JWT Configuration
JWT_SECRET=$(openssl rand -base64 64)  # Сгенерируйте!
JWT_EXPIRATION=86400000

# Encryption Configuration
ENCRYPTION_SECRET_KEY=$(openssl rand -hex 16)  # Должен быть ровно 32 символа!

# Server Configuration
SERVICE_PORT=8091

# Logging
LOG_LEVEL=WARN  # Меньше логов на проде
SHOW_SQL=false
```

#### 2. Генерация безопасных ключей

```bash
# JWT Secret (64 символа base64)
openssl rand -base64 64

# Encryption Key (32 символа hex = 64 hex символа / 2)
openssl rand -hex 16
```

#### 3. Запустите на сервере

```bash
# Убедитесь, что .env файл загружен
docker-compose --env-file .env up -d

# Или с явным указанием
docker-compose config  # Проверить конфигурацию
docker-compose up -d   # Запустить
```

#### 4. Проверьте статус

```bash
docker-compose ps
docker-compose logs ai-service
```

### Вариант 3: Ручной запуск (без Docker Compose)

Если вы запускаете приложение вручную (java -jar), передайте переменные окружения:

```bash
export DB_URL="jdbc:postgresql://YOUR_DB_HOST:5432/ai_integration_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
export JWT_SECRET="your-jwt-secret"
export ENCRYPTION_SECRET_KEY="your-32-character-encryption-key"

java -jar build/libs/noteapp-ai-integration-0.0.1-SNAPSHOT.jar
```

## Диагностика проблем

### 1. Проверка подключения к БД из контейнера

```bash
# Войдите в контейнер приложения
docker exec -it ai-integration-service sh

# Попробуйте пингануть базу
ping postgres

# Должен резолвиться в IP адрес контейнера postgres
```

### 2. Проверка переменных окружения в контейнере

```bash
docker exec -it ai-integration-service printenv | grep DB
```

Должно показать:
```
DB_URL=jdbc:postgresql://postgres:5432/ai_integration_db
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

### 3. Проверка логов PostgreSQL

```bash
docker logs ai-integration-db
```

Должно быть:
```
database system is ready to accept connections
```

### 4. Проверка сети Docker

```bash
docker network inspect ai-integration-network
```

Должны быть оба контейнера: `postgres` и `ai-service`.

### 5. Тест подключения к БД напрямую

```bash
# Установите psql клиент (если еще не установлен)
# Ubuntu/Debian: sudo apt-get install postgresql-client
# Mac: brew install postgresql

# Подключитесь к БД
psql -h localhost -p 5433 -U postgres -d ai_integration_db
```

## Типичные ошибки

### Ошибка 1: localhost:5432 refused
**Причина**: Приложение использует дефолтное значение `localhost:5432`  
**Решение**: Проверьте, что переменная `DB_URL` передается в контейнер

### Ошибка 2: password authentication failed
**Причина**: Неверный пароль в `DB_PASSWORD`  
**Решение**: Убедитесь, что пароли в `.env` и docker-compose.yml совпадают

### Ошибка 3: database "ai_integration_db" does not exist
**Причина**: БД не создана автоматически  
**Решение**: PostgreSQL контейнер должен создать БД при первом запуске. Удалите volume и пересоздайте:
```bash
docker-compose down -v
docker-compose up -d
```

### Ошибка 4: Flyway migration errors
**Причина**: Проблемы с миграциями Flyway  
**Решение**: Проверьте файлы миграций в `src/main/resources/db/migration/`

## Настройка Nginx (если используется)

Если вы используете Nginx как reverse proxy:

```nginx
upstream ai_integration {
    server localhost:8091;
}

server {
    listen 80;
    server_name ai-integration.yourdomain.com;

    location / {
        proxy_pass http://ai_integration;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Для health checks
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    location /actuator/health {
        proxy_pass http://ai_integration/actuator/health;
        access_log off;
    }
}
```

## Мониторинг

### Prometheus + Grafana (опционально)

```bash
# Запустите с мониторингом
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Доступ:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

## Безопасность

### ВАЖНО для продакшена:

1. **Измените все дефолтные пароли и секреты!**
2. **Используйте HTTPS** (добавьте SSL сертификат в Nginx)
3. **Ограничьте доступ к портам** (только через Nginx)
4. **Включите firewall** (ufw/iptables)
5. **Регулярно обновляйте** Docker образы

```bash
# Пример настройки firewall
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 22/tcp    # SSH
sudo ufw enable

# Запретить прямой доступ к портам приложения (5433, 8091)
```

## Backup базы данных

```bash
# Создать backup
docker exec ai-integration-db pg_dump -U postgres ai_integration_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Восстановить из backup
cat backup_20250105_120000.sql | docker exec -i ai-integration-db psql -U postgres -d ai_integration_db
```

## Обновление приложения

```bash
# 1. Создайте backup БД
docker exec ai-integration-db pg_dump -U postgres ai_integration_db > backup.sql

# 2. Остановите контейнеры
docker-compose down

# 3. Обновите код и пересоберите образ
git pull
docker-compose build --no-cache

# 4. Запустите обновленную версию
docker-compose up -d

# 5. Проверьте логи
docker-compose logs -f ai-service
```

## Помощь

Если проблема не решена:

1. Соберите логи:
   ```bash
   docker-compose logs > logs.txt
   ```

2. Проверьте конфигурацию:
   ```bash
   docker-compose config > config.yml
   ```

3. Проверьте статус контейнеров:
   ```bash
   docker-compose ps
   docker stats
   ```

4. Откройте issue на GitHub с логами и конфигурацией

