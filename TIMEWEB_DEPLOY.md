# Деплой AI Integration Service на Timeweb

## ⚠️ Особенности Timeweb

Timeweb накладывает ограничения на `docker-compose.yml`:
- ❌ **Volumes не поддерживаются** - данные БД НЕ сохраняются при перезапуске
- ❌ **Проброс портов БД запрещен** - база доступна только внутри Docker сети
- ✅ **Docker Compose поддерживается** - но с ограничениями

## 🚀 Быстрый деплой (5 минут)

### Шаг 1: Подготовка на локальной машине

```bash
cd noteapp-ai-integration

# Убедитесь, что у вас есть docker-compose.timeweb.yml
ls -la docker-compose.timeweb.yml

# Загрузите на сервер
scp docker-compose.timeweb.yml .env-template user@your-server:/path/to/project/
# Или используйте Git
git add .
git commit -m "Add Timeweb docker-compose"
git push
```

### Шаг 2: На сервере Timeweb

```bash
cd /path/to/project

# Если через Git
git pull

# Создайте .env из шаблона
cp .env-template .env
nano .env
```

**Отредактируйте `.env`:**

```env
# ==========================================
# Database Configuration
# ==========================================
DB_NAME=ai_integration_db
DB_USER=ai_admin
DB_PASSWORD=CHANGE_THIS_PASSWORD_123  # ⚠️ Измените!

# ==========================================
# JWT Configuration
# ==========================================
JWT_SECRET=YOUR_GENERATED_JWT_SECRET_HERE  # ⚠️ Сгенерируйте!
JWT_EXPIRATION=86400000

# ==========================================
# Encryption Configuration (32 символа!)
# ==========================================
ENCRYPTION_SECRET_KEY=CHANGE_THIS_32_CHARS_KEY12345  # ⚠️ Ровно 32!

# ==========================================
# AI Service Configuration
# ==========================================
AI_REQUEST_TIMEOUT=60
AI_MAX_RETRIES=3
AI_ENABLE_FALLBACK=true

# ==========================================
# Server Ports
# ==========================================
SERVICE_PORT=8091
FRONTEND_PORT=3001

# ==========================================
# Frontend API URL
# ==========================================
# ⚠️ ВАЖНО: Укажите ваш домен или IP!
VITE_API_URL=http://YOUR_DOMAIN_OR_IP:8091

# ==========================================
# Logging
# ==========================================
LOG_LEVEL=INFO
SHOW_SQL=false
```

**Сгенерируйте безопасные ключи:**

```bash
# JWT Secret
openssl rand -base64 64

# Encryption Key (ровно 32 символа)
openssl rand -hex 16
```

Скопируйте сгенерированные ключи в `.env`.

### Шаг 3: Запустите через специальный файл

```bash
# ⚠️ ВАЖНО: Используйте docker-compose.timeweb.yml!
docker-compose -f docker-compose.timeweb.yml build

# Запустите все сервисы
docker-compose -f docker-compose.timeweb.yml up -d

# Проверьте статус
docker-compose -f docker-compose.timeweb.yml ps
```

Должно быть:

```
NAME                      STATUS         PORTS
ai-integration-db         Up (healthy)   
ai-integration-service    Up (healthy)   0.0.0.0:8091->8091/tcp
ai-admin-frontend         Up (healthy)   0.0.0.0:3001->80/tcp
```

### Шаг 4: Проверка

```bash
# Backend health
curl http://localhost:8091/actuator/health
# Ожидается: {"status":"UP"}

# Frontend доступен
curl -I http://localhost:3001
# Ожидается: HTTP/1.1 200 OK

# Логи
docker-compose -f docker-compose.timeweb.yml logs -f
```

---

## 🌐 Доступ к сервисам

После успешного запуска:

- **Backend API**: `http://YOUR_IP:8091`
- **Admin Frontend**: `http://YOUR_IP:3001`
- **Swagger UI**: `http://YOUR_IP:8091/swagger-ui/`
- **Health Check**: `http://YOUR_IP:8091/actuator/health`

---

## 🔐 Создание администратора

### Способ 1: Через API

```bash
curl -X POST http://localhost:8091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "your-secure-password"
  }'
```

### Способ 2: Через SQL

```bash
docker exec -it ai-integration-db psql -U ai_admin -d ai_integration_db
```

В psql:

```sql
-- Создайте администратора
INSERT INTO admins (username, password, created_at, updated_at) 
VALUES ('admin', 'admin', NOW(), NOW());

-- Выйдите
\q
```

**⚠️ Пароль `admin` будет захеширован при первом входе. Смените его сразу!**

---

## 💾 ВАЖНО: Backup БД (обязательно!)

⚠️ **КРИТИЧЕСКИ ВАЖНО**: Так как volumes не поддерживаются, данные БД **не сохраняются** при перезапуске контейнера!

### Автоматический backup (cron)

Создайте скрипт backup:

```bash
nano /root/backup-ai-integration.sh
```

Вставьте:

```bash
#!/bin/bash
BACKUP_DIR="/root/backups/ai-integration"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup БД
docker exec ai-integration-db pg_dump -U ai_admin ai_integration_db \
  > $BACKUP_DIR/ai_integration_$DATE.sql

# Сжатие
gzip $BACKUP_DIR/ai_integration_$DATE.sql

# Удаление старых backup (старше 7 дней)
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "✅ Backup completed: $BACKUP_DIR/ai_integration_$DATE.sql.gz"
```

Сделайте исполняемым:

```bash
chmod +x /root/backup-ai-integration.sh
```

Добавьте в cron (каждый день в 3:00):

```bash
crontab -e
```

Добавьте строку:

```
0 3 * * * /root/backup-ai-integration.sh >> /var/log/ai-backup.log 2>&1
```

### Ручной backup

```bash
# Создать backup
docker exec ai-integration-db pg_dump -U ai_admin ai_integration_db \
  > backup_$(date +%Y%m%d_%H%M%S).sql

# Сжать
gzip backup_*.sql

# Скачать на локальную машину
scp user@server:/path/to/backup_*.sql.gz ./
```

### Восстановление из backup

```bash
# Загрузите backup на сервер
scp backup.sql.gz user@server:/tmp/

# На сервере
gunzip /tmp/backup.sql.gz

# Восстановите
cat /tmp/backup.sql | docker exec -i ai-integration-db \
  psql -U ai_admin -d ai_integration_db

# Или
docker cp /tmp/backup.sql ai-integration-db:/tmp/
docker exec ai-integration-db psql -U ai_admin -d ai_integration_db -f /tmp/backup.sql
```

---

## 🔧 Управление сервисами

### Все команды используют `-f docker-compose.timeweb.yml`!

```bash
# Статус
docker-compose -f docker-compose.timeweb.yml ps

# Логи
docker-compose -f docker-compose.timeweb.yml logs -f

# Перезапуск
docker-compose -f docker-compose.timeweb.yml restart

# Остановка
docker-compose -f docker-compose.timeweb.yml down

# Запуск
docker-compose -f docker-compose.timeweb.yml up -d

# Пересборка
docker-compose -f docker-compose.timeweb.yml build --no-cache
docker-compose -f docker-compose.timeweb.yml up -d
```

### Для удобства создайте alias:

```bash
echo 'alias dcai="docker-compose -f docker-compose.timeweb.yml"' >> ~/.bashrc
source ~/.bashrc

# Теперь можно использовать:
dcai ps
dcai logs -f
dcai restart
```

---

## 🔄 Обновление сервисов

### Обновление кода

```bash
# Backup БД перед обновлением!
docker exec ai-integration-db pg_dump -U ai_admin ai_integration_db > backup_before_update.sql

# Остановите
docker-compose -f docker-compose.timeweb.yml down

# Обновите код
git pull

# Пересоберите
docker-compose -f docker-compose.timeweb.yml build --no-cache

# Запустите
docker-compose -f docker-compose.timeweb.yml up -d

# Проверьте логи
docker-compose -f docker-compose.timeweb.yml logs -f
```

### Обновление только frontend

```bash
cd frontend
git pull  # Или обновите файлы

cd ..
docker-compose -f docker-compose.timeweb.yml build ai-admin-frontend
docker-compose -f docker-compose.timeweb.yml up -d ai-admin-frontend
```

---

## 🌐 Изменение API URL фронтенда

Если нужно изменить URL, на который фронтенд обращается к backend:

### Вариант 1: Через .env (рекомендуется)

```bash
nano .env
```

Измените:
```env
VITE_API_URL=http://your-domain.com:8091
```

Пересоберите:
```bash
docker-compose -f docker-compose.timeweb.yml build ai-admin-frontend
docker-compose -f docker-compose.timeweb.yml up -d ai-admin-frontend
```

### Вариант 2: Через build argument

```bash
docker-compose -f docker-compose.timeweb.yml build \
  --build-arg VITE_API_URL=http://your-domain.com:8091 \
  ai-admin-frontend

docker-compose -f docker-compose.timeweb.yml up -d ai-admin-frontend
```

---

## 🔥 Firewall (если используется)

```bash
# Разрешите порты
sudo ufw allow 8091/tcp   # Backend API
sudo ufw allow 3001/tcp   # Frontend Admin

# Проверьте
sudo ufw status
```

---

## 🐛 Troubleshooting

### Проблема: "Sanitizer check error volumes is not allowed"

**Причина**: Используется обычный `docker-compose.yml` вместо `docker-compose.timeweb.yml`

**Решение**: Используйте `-f docker-compose.timeweb.yml`:

```bash
docker-compose -f docker-compose.timeweb.yml up -d
```

### Проблема: Данные БД пропали после перезапуска

**Причина**: Volumes не поддерживаются на Timeweb

**Решение**: 
1. Восстановите из backup
2. Настройте автоматический backup (см. выше)
3. Делайте backup **перед каждым** `docker-compose down`

### Проблема: Frontend не может подключиться к backend

**Симптомы в браузере (DevTools):**
```
Failed to fetch http://localhost:8091/api/...
```

**Решение**: Укажите правильный `VITE_API_URL` в `.env`:

```bash
nano .env
# Измените VITE_API_URL на ваш домен или IP

# Пересоберите frontend
docker-compose -f docker-compose.timeweb.yml build ai-admin-frontend
docker-compose -f docker-compose.timeweb.yml up -d ai-admin-frontend
```

### Проблема: CORS ошибки

**Решение**: Backend должен разрешать запросы с вашего домена.

Проверьте, что в `application.yml` есть:

```yaml
cors:
  allowed-origins:
    - http://YOUR_DOMAIN:3001
    - http://YOUR_IP:3001
```

Если нет, добавьте в `.env`:

```env
CORS_ALLOWED_ORIGINS=http://YOUR_DOMAIN:3001,http://YOUR_IP:3001
```

---

## 📊 Мониторинг

```bash
# Статус контейнеров
docker-compose -f docker-compose.timeweb.yml ps

# Логи (real-time)
docker-compose -f docker-compose.timeweb.yml logs -f

# Логи конкретного сервиса
docker logs ai-integration-service -f
docker logs ai-admin-frontend -f
docker logs ai-integration-db -f

# Статистика ресурсов
docker stats

# Health checks
curl http://localhost:8091/actuator/health
curl -I http://localhost:3001
```

---

## 📚 Настройка noteapp

После успешного запуска AI Integration:

1. Откройте админку: `http://YOUR_IP:3001`
2. Войдите как admin
3. Перейдите в **Clients** → **Add Client**
4. Создайте клиента `noteapp`
5. **Скопируйте API Key**

В `.env` файле noteapp:

```env
AI_INTEGRATION_BASE_URL=http://YOUR_IP:8091
AI_INTEGRATION_API_KEY=sk_xxxxxxxxxxxxxx
AI_INTEGRATION_TIMEOUT=30000
AI_INTEGRATION_RETRY_ATTEMPTS=3
```

Перезапустите noteapp:

```bash
docker-compose restart noteapp
```

Подробнее: [NOTEAPP_INTEGRATION_GUIDE.md](NOTEAPP_INTEGRATION_GUIDE.md)

---

## ✅ Контрольный чеклист

- [ ] Создан `.env` с правильными значениями
- [ ] Сгенерированы безопасные JWT_SECRET и ENCRYPTION_SECRET_KEY
- [ ] Указан правильный VITE_API_URL
- [ ] Запущено: `docker-compose -f docker-compose.timeweb.yml up -d`
- [ ] Все контейнеры в статусе "Up"
- [ ] Backend доступен: `curl http://localhost:8091/actuator/health`
- [ ] Frontend доступен: `curl http://localhost:3001`
- [ ] Создан администратор
- [ ] Настроен автоматический backup БД
- [ ] Создан клиент для noteapp
- [ ] noteapp настроен и подключен

---

## 🆘 Быстрая помощь

**Всё упало после перезапуска?**
```bash
# Восстановите из последнего backup
cat backup_latest.sql | docker exec -i ai-integration-db \
  psql -U ai_admin -d ai_integration_db
```

**Нужно начать с чистого листа?**
```bash
docker-compose -f docker-compose.timeweb.yml down
docker system prune -a --volumes
docker-compose -f docker-compose.timeweb.yml build --no-cache
docker-compose -f docker-compose.timeweb.yml up -d
```

**Проверить, что volumes действительно не используются?**
```bash
docker-compose -f docker-compose.timeweb.yml config | grep volumes
# Не должно быть volumes секции
```

---

## 🎉 Готово!

Теперь у вас работает AI Integration Service на Timeweb без volumes! 

**НЕ ЗАБЫВАЙТЕ**: Регулярно делайте backup БД! 💾

Следующий шаг: [Настройка noteapp](NOTEAPP_INTEGRATION_GUIDE.md)

