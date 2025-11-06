# Деплой AI Integration на Timeweb БЕЗ Nginx

## Для кого эта инструкция?

Если у вас **НЕТ доступа к настройке Nginx** на Timeweb (shared hosting, ограниченный доступ), эта инструкция для вас!

Всё будет работать через Docker Compose - и backend, и frontend, и база данных.

---

## 🚀 Быстрый старт (5 минут)

### Шаг 1: Подготовьте файлы на локальной машине

```bash
cd noteapp-ai-integration

# Убедитесь, что у вас есть все файлы:
# - frontend/Dockerfile ✅
# - frontend/nginx.conf ✅
# - docker-compose.yml (обновлен) ✅
```

### Шаг 2: Загрузите проект на сервер

```bash
# Архивируйте проект
tar -czf ai-integration.tar.gz .

# Загрузите на Timeweb
scp ai-integration.tar.gz user@your-server:/path/to/

# Или используйте Git
git add .
git commit -m "Add frontend Docker setup"
git push

# На сервере
cd /path/to/
git pull  # Или распакуйте архив
```

### Шаг 3: Создайте .env на сервере

```bash
# На сервере Timeweb
cd /path/to/noteapp-ai-integration
nano .env
```

Вставьте конфигурацию:

```env
# ==========================================
# Database Configuration
# ==========================================
DB_NAME=ai_integration_db
DB_USER=postgres
DB_PASSWORD=CHANGE_THIS_PASSWORD  # Измените!
DB_PORT=5433

# ==========================================
# JWT Configuration
# ==========================================
JWT_SECRET=CHANGE_THIS_TO_SECURE_KEY  # Сгенерируйте!
JWT_EXPIRATION=86400000

# ==========================================
# Encryption Configuration
# ==========================================
ENCRYPTION_SECRET_KEY=CHANGE_THIS_32_CHARS_KEY12  # Ровно 32 символа!

# ==========================================
# AI Service Configuration
# ==========================================
AI_REQUEST_TIMEOUT=60
AI_MAX_RETRIES=3
AI_ENABLE_FALLBACK=true

# ==========================================
# Server Ports
# ==========================================
SERVICE_PORT=8091      # Backend API
FRONTEND_PORT=3001     # Admin Frontend

# ==========================================
# Logging
# ==========================================
LOG_LEVEL=INFO
SHOW_SQL=false
```

Сохраните (Ctrl+O, Enter, Ctrl+X).

### Шаг 4: Соберите и запустите все сервисы

```bash
# Соберите образы (первый раз может занять 5-10 минут)
docker-compose build

# Запустите все контейнеры
docker-compose up -d

# Проверьте статус
docker-compose ps
```

Должно быть:

```
NAME                      STATUS                 PORTS
ai-integration-db         Up (healthy)           0.0.0.0:5433->5432/tcp
ai-integration-service    Up (healthy)           0.0.0.0:8091->8091/tcp
ai-admin-frontend         Up (healthy)           0.0.0.0:3001->80/tcp
```

### Шаг 5: Проверьте, что всё работает

```bash
# Backend health
curl http://localhost:8091/actuator/health
# Ожидается: {"status":"UP"}

# Frontend доступен
curl http://localhost:3001
# Ожидается: HTML страница

# Проверьте логи
docker-compose logs -f
```

---

## 🌐 Доступ к сервисам

После успешного запуска сервисы доступны по адресам:

### Если у вас IP адрес:
- **Backend API**: `http://YOUR_SERVER_IP:8091`
- **Admin Frontend**: `http://YOUR_SERVER_IP:3001`
- **Swagger UI**: `http://YOUR_SERVER_IP:8091/swagger-ui/`

### Если у вас домен:
- **Backend API**: `http://your-domain.com:8091`
- **Admin Frontend**: `http://your-domain.com:3001`

### Для локального тестирования:
- **Backend API**: `http://localhost:8091`
- **Admin Frontend**: `http://localhost:3001`

---

## 🔧 Настройка API URL для фронтенда

По умолчанию фронтенд будет обращаться к backend на `http://localhost:8091`.

Если вы хотите изменить это:

### Вариант 1: Через build argument при сборке

```bash
# Остановите контейнеры
docker-compose down

# Пересоберите с custom API URL
docker-compose build --build-arg VITE_API_URL=http://your-domain.com:8091 ai-admin-frontend

# Запустите
docker-compose up -d
```

### Вариант 2: Обновите Dockerfile

Отредактируйте `frontend/Dockerfile`, строка с ARG:

```dockerfile
ARG VITE_API_URL=http://your-domain.com:8091
```

Пересоберите:

```bash
docker-compose build ai-admin-frontend
docker-compose up -d ai-admin-frontend
```

---

## 🔐 Первый вход в админку

### 1. Создайте администратора

**Вариант А: Через API**

```bash
curl -X POST http://localhost:8091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "your-secure-password"
  }'
```

**Вариант Б: Через SQL**

```bash
# Войдите в контейнер БД
docker exec -it ai-integration-db psql -U postgres -d ai_integration_db

# Создайте админа (пароль будет захеширован при первом входе)
INSERT INTO admins (username, password, created_at, updated_at) 
VALUES ('admin', 'admin', NOW(), NOW());

# Выйдите
\q
```

### 2. Войдите в админку

Откройте в браузере:
```
http://your-server-ip:3001
```

Введите:
- **Username**: `admin`
- **Password**: ваш пароль

---

## 🔌 Настройка noteapp для подключения

### Шаг 1: Создайте клиента в админке

1. Откройте админку: `http://your-server:3001`
2. Перейдите в **Clients** → **Add Client**
3. Name: `noteapp`, Description: `Main note app`
4. **Скопируйте API Key**: `sk_xxxxxx...`

### Шаг 2: Настройте noteapp

В `.env` файле noteapp добавьте:

```env
# AI Integration
AI_INTEGRATION_BASE_URL=http://your-server-ip:8091
AI_INTEGRATION_API_KEY=sk_xxxxxx...
AI_INTEGRATION_TIMEOUT=30000
AI_INTEGRATION_RETRY_ATTEMPTS=3
```

### Шаг 3: Перезапустите noteapp

```bash
docker-compose restart noteapp

# Проверьте логи
docker logs noteapp -f
# Ожидается: "✅ [AI Integration] Подключение к сервису..."
```

---

## 🔥 Firewall и безопасность

### Открытие портов

Если используете firewall (UFW):

```bash
# Разрешите порты
sudo ufw allow 8091/tcp   # Backend API
sudo ufw allow 3001/tcp   # Frontend
sudo ufw allow 22/tcp     # SSH (если еще не открыт)

# Проверьте статус
sudo ufw status
```

### Закрытие порта БД

База данных должна быть доступна только внутри Docker сети:

```bash
# Порт 5433 НЕ должен быть открыт наружу
# В docker-compose.yml закомментируйте:
# ports:
#   - "5433:5432"  # <-- Закомментируйте эту строку!
```

---

## 📊 Мониторинг

### Проверка статуса контейнеров

```bash
# Статус всех контейнеров
docker-compose ps

# Логи всех сервисов
docker-compose logs -f

# Логи конкретного сервиса
docker logs ai-integration-service -f
docker logs ai-admin-frontend -f
docker logs ai-integration-db -f
```

### Health checks

```bash
# Backend
curl http://localhost:8091/actuator/health

# Frontend (должен вернуть HTML)
curl http://localhost:3001

# Database
docker exec -it ai-integration-db pg_isready -U postgres
```

### Использование ресурсов

```bash
# Статистика Docker
docker stats

# Использование дискового пространства
docker system df
```

---

## 🔄 Обновление сервисов

### Обновление backend

```bash
# Остановите backend
docker-compose stop ai-service

# Пересоберите
docker-compose build ai-service

# Запустите
docker-compose up -d ai-service

# Проверьте логи
docker logs ai-integration-service -f
```

### Обновление frontend

```bash
# Если изменился код фронтенда
cd frontend
git pull  # Или обновите файлы

# Пересоберите
cd ..
docker-compose build ai-admin-frontend

# Запустите
docker-compose up -d ai-admin-frontend
```

### Обновление всего

```bash
# Остановите все
docker-compose down

# Обновите код
git pull

# Пересоберите все
docker-compose build --no-cache

# Запустите
docker-compose up -d

# Проверьте
docker-compose ps
```

---

## 🐛 Troubleshooting

### Проблема: Фронтенд не может подключиться к backend

**Симптомы в браузере (DevTools → Console):**
```
Failed to fetch http://localhost:8091/api/...
```

**Причина**: Неправильный API URL в фронтенде

**Решение 1**: Измените VITE_API_URL при сборке

```bash
docker-compose down
docker-compose build --build-arg VITE_API_URL=http://YOUR_IP:8091 ai-admin-frontend
docker-compose up -d
```

**Решение 2**: Проверьте, что backend доступен

```bash
# С сервера
curl http://localhost:8091/actuator/health

# С вашего компьютера
curl http://YOUR_SERVER_IP:8091/actuator/health
```

### Проблема: CORS ошибки в браузере

**Симптомы:**
```
Access to fetch at 'http://...' from origin 'http://...' has been blocked by CORS policy
```

**Решение**: Backend должен разрешить CORS для фронтенда

В `application.yml` backend должно быть:

```yaml
# Это уже есть в проекте, но проверьте
cors:
  allowed-origins: 
    - http://localhost:3001
    - http://your-domain.com:3001
```

### Проблема: Контейнер постоянно перезапускается

```bash
# Проверьте логи
docker logs ai-admin-frontend --tail 50

# Проверьте health check
docker inspect ai-admin-frontend | grep -A 5 Health
```

**Частая причина**: Не установлен curl в образе

**Решение**: В `frontend/Dockerfile` должно быть:

```dockerfile
RUN apk add --no-cache curl
```

---

## 📦 Backup и восстановление

### Backup базы данных

```bash
# Создать backup
docker exec ai-integration-db pg_dump -U postgres ai_integration_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Или сохранить внутри контейнера
docker exec ai-integration-db pg_dump -U postgres ai_integration_db > /tmp/backup.sql
docker cp ai-integration-db:/tmp/backup.sql ./backup.sql
```

### Восстановление

```bash
# Восстановить из файла
cat backup.sql | docker exec -i ai-integration-db psql -U postgres -d ai_integration_db

# Или
docker cp backup.sql ai-integration-db:/tmp/backup.sql
docker exec -i ai-integration-db psql -U postgres -d ai_integration_db < /tmp/backup.sql
```

---

## 🚀 Производительность и оптимизация

### Уменьшение размера образов

```bash
# Очистите неиспользуемые образы
docker image prune -a

# Очистите volumes (ОСТОРОЖНО - удалит данные!)
docker volume prune
```

### Настройка логирования

Чтобы логи не занимали много места, добавьте в `docker-compose.yml`:

```yaml
services:
  ai-service:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

---

## ✅ Контрольный чеклист

- [ ] Создан `.env` файл на сервере
- [ ] Все сервисы запущены (`docker-compose ps`)
- [ ] Backend доступен (`curl http://localhost:8091/actuator/health`)
- [ ] Frontend доступен (`curl http://localhost:3001`)
- [ ] Создан администратор
- [ ] Вход в админку работает
- [ ] Создан клиент для noteapp
- [ ] API Key скопирован
- [ ] noteapp настроен и подключен
- [ ] Нейросети синхронизированы
- [ ] Firewall настроен
- [ ] Backup БД настроен

---

## 📚 Дополнительные ресурсы

- **Настройка noteapp**: [NOTEAPP_INTEGRATION_GUIDE.md](NOTEAPP_INTEGRATION_GUIDE.md)
- **Полный деплой гайд**: [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- **Быстрая памятка**: [TIMEWEB_QUICK_REFERENCE.md](TIMEWEB_QUICK_REFERENCE.md)

---

## 🎉 Готово!

Теперь у вас работает:
- ✅ Backend API на порту 8091
- ✅ Admin Frontend на порту 3001
- ✅ PostgreSQL в Docker сети

Всё это БЕЗ ручной настройки Nginx! 🚀

**Следующий шаг**: Настройте noteapp ([инструкция](NOTEAPP_INTEGRATION_GUIDE.md))

