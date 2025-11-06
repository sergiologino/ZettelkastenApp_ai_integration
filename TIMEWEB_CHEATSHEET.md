# Timeweb - Шпаргалка команд

## ⚠️ ВАЖНО: Всегда используйте `-f docker-compose.timeweb.yml`!

```bash
# Создайте alias для удобства
echo 'alias dcai="docker-compose -f docker-compose.timeweb.yml"' >> ~/.bashrc
source ~/.bashrc
```

## 🚀 Первый запуск

```bash
# 1. Создайте .env
cp .env-template .env
nano .env  # Измените пароли и секреты!

# 2. Соберите
docker-compose -f docker-compose.timeweb.yml build

# 3. Запустите
docker-compose -f docker-compose.timeweb.yml up -d

# 4. Проверьте
docker-compose -f docker-compose.timeweb.yml ps
curl http://localhost:8091/actuator/health
```

## 🔧 Управление (с alias)

```bash
dcai ps           # Статус
dcai logs -f      # Логи
dcai restart      # Перезапуск всех
dcai down         # Остановка
dcai up -d        # Запуск
```

## 💾 BACKUP (КРИТИЧЕСКИ ВАЖНО!)

```bash
# Ручной backup
docker exec ai-integration-db pg_dump -U ai_admin ai_integration_db > backup_$(date +%Y%m%d).sql

# Автоматический backup (каждый день в 3:00)
cat > /root/backup-ai.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/root/backups/ai-integration"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
docker exec ai-integration-db pg_dump -U ai_admin ai_integration_db | gzip > $BACKUP_DIR/ai_$DATE.sql.gz
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
echo "✅ Backup: $BACKUP_DIR/ai_$DATE.sql.gz"
EOF
chmod +x /root/backup-ai.sh
echo "0 3 * * * /root/backup-ai.sh >> /var/log/ai-backup.log 2>&1" | crontab -

# Восстановление
cat backup.sql | docker exec -i ai-integration-db psql -U ai_admin -d ai_integration_db
```

## 🔄 Обновление

```bash
# ВСЕГДА делайте backup перед обновлением!
docker exec ai-integration-db pg_dump -U ai_admin ai_integration_db > backup_before_update.sql

# Обновление
dcai down
git pull
dcai build --no-cache
dcai up -d
dcai logs -f
```

## 🌐 Изменение API URL фронтенда

```bash
# В .env измените
nano .env
# VITE_API_URL=http://YOUR_DOMAIN:8091

# Пересоберите frontend
dcai build ai-admin-frontend
dcai up -d ai-admin-frontend
```

## 🔐 Создание админа

```bash
# Через API
curl -X POST http://localhost:8091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your-password"}'

# Через SQL
docker exec -it ai-integration-db psql -U ai_admin -d ai_integration_db -c \
  "INSERT INTO admins (username,password,created_at,updated_at) VALUES ('admin','admin',NOW(),NOW());"
```

## 📊 Мониторинг

```bash
dcai ps                                    # Статус
dcai logs -f                              # Все логи
docker logs ai-integration-service -f     # Backend
docker logs ai-admin-frontend -f          # Frontend
docker logs ai-integration-db -f          # БД
docker stats                              # Ресурсы
```

## 🐛 Troubleshooting

```bash
# Проблема: "volumes is not allowed"
# Решение: Используйте docker-compose.timeweb.yml!
dcai up -d  # ✅ Правильно
docker-compose up -d  # ❌ Неправильно

# Проблема: Данные БД пропали
# Решение: Восстановите из backup
cat backup_latest.sql | docker exec -i ai-integration-db psql -U ai_admin -d ai_integration_db

# Проблема: Frontend не подключается
# Решение: Измените VITE_API_URL в .env
nano .env
dcai build ai-admin-frontend
dcai up -d ai-admin-frontend

# Проблема: Всё сломалось
# Решение: Начните с чистого листа
dcai down
docker system prune -a --volumes
dcai build --no-cache
dcai up -d
```

## 🔥 Firewall

```bash
sudo ufw allow 8091/tcp   # Backend
sudo ufw allow 3001/tcp   # Frontend
sudo ufw status
```

## 📦 Полная перезагрузка

```bash
# BACKUP СНАЧАЛА!
docker exec ai-integration-db pg_dump -U ai_admin ai_integration_db > backup.sql

# Удалите всё
dcai down
docker system prune -a --volumes

# Пересоздайте
dcai build --no-cache
dcai up -d

# Восстановите данные
cat backup.sql | docker exec -i ai-integration-db psql -U ai_admin -d ai_integration_db
```

## 🆘 Быстрая помощь

- **Доступ**: `http://YOUR_IP:8091` (backend), `http://YOUR_IP:3001` (frontend)
- **Полная инструкция**: [TIMEWEB_DEPLOY.md](TIMEWEB_DEPLOY.md)
- **Настройка noteapp**: [NOTEAPP_INTEGRATION_GUIDE.md](NOTEAPP_INTEGRATION_GUIDE.md)

