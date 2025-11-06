# Docker-only деплой - Шпаргалка команд

## 🚀 Первоначальная настройка

```bash
# 1. Создайте .env
cat > .env << 'EOF'
DB_NAME=ai_integration_db
DB_USER=postgres
DB_PASSWORD=your-password
DB_PORT=5433
JWT_SECRET=$(openssl rand -base64 64)
ENCRYPTION_SECRET_KEY=$(openssl rand -hex 16)
SERVICE_PORT=8091
FRONTEND_PORT=3001
LOG_LEVEL=INFO
SHOW_SQL=false
EOF

# 2. Соберите и запустите
docker-compose build
docker-compose up -d

# 3. Проверьте статус
docker-compose ps
```

## ✅ Проверка работоспособности

```bash
# Backend health
curl http://localhost:8091/actuator/health

# Frontend (должен вернуть HTML)
curl -I http://localhost:3001

# Все логи
docker-compose logs -f
```

## 🔧 Управление сервисами

```bash
# Перезапуск всех
docker-compose restart

# Перезапуск конкретного
docker-compose restart ai-service
docker-compose restart ai-admin-frontend

# Остановка всех
docker-compose down

# Остановка с удалением volumes (ОСТОРОЖНО!)
docker-compose down -v

# Старт
docker-compose up -d
```

## 📊 Мониторинг

```bash
# Статус контейнеров
docker-compose ps

# Логи backend
docker logs ai-integration-service -f

# Логи frontend
docker logs ai-admin-frontend -f

# Логи БД
docker logs ai-integration-db -f

# Последние 50 строк
docker logs --tail 50 ai-integration-service

# Статистика ресурсов
docker stats
```

## 🔄 Обновление

```bash
# Обновить код
git pull

# Пересобрать всё
docker-compose build --no-cache

# Перезапустить
docker-compose down
docker-compose up -d

# Только frontend
docker-compose build ai-admin-frontend
docker-compose up -d ai-admin-frontend

# Только backend
docker-compose build ai-service
docker-compose up -d ai-service
```

## 🌐 Изменение API URL фронтенда

```bash
# Способ 1: Build argument
docker-compose build \
  --build-arg VITE_API_URL=http://your-domain.com:8091 \
  ai-admin-frontend

# Способ 2: Измените frontend/Dockerfile
nano frontend/Dockerfile
# Измените строку: ARG VITE_API_URL=http://your-domain.com:8091

docker-compose build ai-admin-frontend
docker-compose up -d ai-admin-frontend
```

## 🔐 Создание администратора

```bash
# Через API
curl -X POST http://localhost:8091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "your-password"}'

# Через SQL
docker exec -it ai-integration-db psql -U postgres -d ai_integration_db -c \
  "INSERT INTO admins (username, password, created_at, updated_at) 
   VALUES ('admin', 'admin', NOW(), NOW());"
```

## 💾 Backup и восстановление

```bash
# Backup
docker exec ai-integration-db pg_dump -U postgres ai_integration_db \
  > backup_$(date +%Y%m%d).sql

# Restore
cat backup.sql | docker exec -i ai-integration-db \
  psql -U postgres -d ai_integration_db
```

## 🐛 Troubleshooting

```bash
# Проверить переменные окружения
docker exec ai-integration-service printenv | grep DB

# Проверить сеть
docker network inspect ai-integration-network

# Пересоздать всё с нуля
docker-compose down -v
docker system prune -a --volumes  # ОСТОРОЖНО!
docker-compose build --no-cache
docker-compose up -d

# Проверить health check
docker inspect ai-admin-frontend | grep -A 10 Health
```

## 🔥 Firewall

```bash
# Открыть порты
sudo ufw allow 8091/tcp   # Backend
sudo ufw allow 3001/tcp   # Frontend
sudo ufw status

# Или для конкретного IP
sudo ufw allow from YOUR_IP to any port 8091
sudo ufw allow from YOUR_IP to any port 3001
```

## 📦 Очистка

```bash
# Остановить и удалить контейнеры
docker-compose down

# Удалить неиспользуемые образы
docker image prune -a

# Удалить volumes (ОСТОРОЖНО - удалит данные БД!)
docker volume prune

# Полная очистка Docker
docker system prune -a --volumes
```

## 🆘 Быстрые фиксы

### Frontend не может подключиться к backend

```bash
# Пересоберите с правильным URL
docker-compose down
docker-compose build --build-arg VITE_API_URL=http://$(hostname -I | awk '{print $1}'):8091 ai-admin-frontend
docker-compose up -d
```

### Backend не запускается (Connection refused)

```bash
# Проверьте .env
cat .env | grep DB_URL
# Должно быть пусто! URL формируется в docker-compose.yml

# Проверьте docker-compose
docker-compose config | grep DB_URL
# Должно быть: jdbc:postgresql://postgres:5432/ai_integration_db
```

### Контейнер постоянно перезапускается

```bash
# Смотрите логи
docker logs ai-admin-frontend --tail 100
docker logs ai-integration-service --tail 100

# Проверьте health check
docker inspect ai-admin-frontend | grep -A 10 Health
```

## 📚 Доступ к сервисам

- Backend: `http://YOUR_IP:8091`
- Frontend: `http://YOUR_IP:3001`
- Swagger: `http://YOUR_IP:8091/swagger-ui/`
- Health: `http://YOUR_IP:8091/actuator/health`

