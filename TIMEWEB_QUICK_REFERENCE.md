# AI Integration + noteapp на Timeweb - Быстрая памятка

## 📋 Краткий чеклист настройки

### Часть 1: AI Integration Backend (уже развернут ✅)

```bash
# 1. Создайте .env файл
cat > .env << 'EOF'
DB_NAME=ai_integration_db
DB_USER=postgres
DB_PASSWORD=your-password
DB_PORT=5433
JWT_SECRET=$(openssl rand -base64 64)
ENCRYPTION_SECRET_KEY=$(openssl rand -hex 16)
SERVICE_PORT=8091
EOF

# 2. Запустите
docker-compose up -d

# 3. Проверьте
curl http://localhost:8091/actuator/health
```

---

### Часть 2: AI Integration Frontend

#### Быстрый способ (Nginx + статика):

```bash
# 1. Локально соберите фронтенд
cd frontend
npm install
echo "VITE_API_URL=https://your-domain.com" > .env
npm run build

# 2. Загрузите на сервер
scp -r dist/* user@server:/var/www/ai-admin/

# 3. Настройте Nginx
sudo nano /etc/nginx/sites-available/ai-admin
```

**Nginx конфиг:**
```nginx
server {
    listen 80;
    server_name ai-admin.your-domain.com;
    root /var/www/ai-admin;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8091/api/;
        proxy_set_header Host $host;
    }
}
```

```bash
# 4. Активируйте
sudo ln -s /etc/nginx/sites-available/ai-admin /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# 5. SSL
sudo certbot --nginx -d ai-admin.your-domain.com
```

---

### Часть 3: Настройка noteapp

#### 1. Получите API Key

1. Откройте `https://ai-admin.your-domain.com`
2. Войдите как admin
3. Clients → Add Client → Name: `noteapp`
4. Скопируйте **API Key**: `sk_xxxxx...`

#### 2. Настройте noteapp

```bash
# В папке noteapp создайте/обновите .env
cat >> .env << 'EOF'

# AI Integration
AI_INTEGRATION_BASE_URL=https://your-ai-domain.com
AI_INTEGRATION_API_KEY=sk_xxxxx...
AI_INTEGRATION_TIMEOUT=30000
AI_INTEGRATION_RETRY_ATTEMPTS=3
EOF

# Перезапустите noteapp
docker-compose restart noteapp

# Проверьте логи
docker logs noteapp -f
# Ожидается: "✅ [AI Integration] Подключение к сервису..."
```

#### 3. Проверьте интеграцию

```bash
# Запрос к noteapp
curl http://localhost:8080/api/neural-networks

# Должен вернуть список нейросетей
```

---

## 🚀 Команды для управления

### AI Integration Service

```bash
# Логи
docker logs ai-integration-service -f

# Перезапуск
docker-compose restart ai-service

# Полная перезагрузка
docker-compose down
docker-compose up -d

# Health check
curl http://localhost:8091/actuator/health

# Swagger UI
open http://localhost:8091/swagger-ui/
```

### noteapp

```bash
# Логи
docker logs noteapp -f

# Перезапуск
docker-compose restart noteapp

# Проверка нейросетей
curl http://localhost:8080/api/neural-networks

# Синхронизация нейросетей
curl -X POST http://localhost:8080/api/neural-networks/sync
```

### Frontend

```bash
# Пересборка и деплой
cd frontend
npm run build
rsync -avz dist/ user@server:/var/www/ai-admin/

# Docker (если используется)
docker-compose build ai-admin-frontend --no-cache
docker-compose up -d ai-admin-frontend
```

---

## 🔧 Частые задачи

### Добавить новую нейросеть

1. Админка → Networks → Add Network
2. Заполните:
   - Name: `whisper-1`
   - Display Name: `Whisper (OpenAI)`
   - Provider: `openai`
   - Type: `transcription`
   - Model: `whisper-1`
3. Настройте API ключ провайдера
4. Save

Автоматически синхронизируется с noteapp.

### Создать нового клиента

1. Админка → Clients → Add Client
2. Name + Description
3. Скопируйте API Key
4. Настройте в приложении-клиенте

### Сменить API Key клиента

1. Админка → Clients → Найти клиента
2. Regenerate Key
3. Скопируйте новый key
4. Обновите `.env` в клиенте
5. Перезапустите клиента

### Просмотр логов запросов

1. Админка → Logs
2. Фильтры:
   - По клиенту
   - По нейросети
   - По статусу (success/error)
3. Экспорт в CSV (если нужно)

### Просмотр статистики

1. Админка → Dashboard
2. Метрики:
   - Запросов за день/неделю/месяц
   - По клиентам
   - По нейросетям
   - Использование токенов

---

## 🔐 Безопасность

### Генерация безопасных ключей

```bash
# JWT Secret (64 символа)
openssl rand -base64 64

# Encryption Key (32 символа)
openssl rand -hex 16

# Пароль БД
openssl rand -base64 24
```

### Обновление секретов

```bash
# 1. Сгенерируйте новые
JWT_SECRET=$(openssl rand -base64 64)
ENCRYPTION_KEY=$(openssl rand -hex 16)

# 2. Обновите .env
nano .env

# 3. Перезапустите
docker-compose down
docker-compose up -d
```

---

## 🐛 Troubleshooting

### Backend не запускается

```bash
# Проверьте логи
docker logs ai-integration-service

# Проверьте БД
docker logs ai-integration-db
docker exec -it ai-integration-db psql -U postgres -l

# Проверьте переменные окружения
docker exec ai-integration-service printenv | grep DB

# Пересоздайте контейнеры
docker-compose down -v
docker-compose up -d
```

### Frontend не подключается к Backend

```bash
# Проверьте VITE_API_URL
cat frontend/.env

# Проверьте Nginx прокси
sudo nginx -t
sudo tail -f /var/log/nginx/error.log

# Проверьте CORS
curl -H "Origin: https://frontend.com" \
     -H "Access-Control-Request-Method: POST" \
     -X OPTIONS http://localhost:8091/api/auth/login
```

### noteapp не видит нейросети

```bash
# Проверьте API Key
docker logs noteapp | grep "AI Integration"

# Принудительная синхронизация
curl -X POST http://localhost:8080/api/neural-networks/sync

# Проверьте клиента в админке
# Clients → noteapp → Is Active = ✅
```

### 401 Unauthorized ошибки

```bash
# 1. Проверьте API Key в .env
cat .env | grep AI_INTEGRATION_API_KEY

# 2. Проверьте клиента в админке
# Clients → noteapp → Status

# 3. Regenerate key если потеряли
```

---

## 📊 Мониторинг

### Базовые health checks

```bash
# AI Integration
curl http://localhost:8091/actuator/health

# noteapp
curl http://localhost:8080/actuator/health

# PostgreSQL
docker exec -it ai-integration-db pg_isready -U postgres
```

### Prometheus метрики (если настроены)

```bash
# AI Integration метрики
curl http://localhost:8091/actuator/prometheus

# noteapp метрики
curl http://localhost:8080/actuator/prometheus
```

### Логи

```bash
# Все логи
docker-compose logs -f

# Только backend
docker logs ai-integration-service -f

# Только БД
docker logs ai-integration-db -f

# Последние 100 строк
docker logs --tail 100 ai-integration-service
```

---

## 📚 Полные инструкции

- **Деплой backend**: [QUICK_START.md](QUICK_START.md)
- **Деплой frontend**: [FRONTEND_DEPLOY_GUIDE.md](FRONTEND_DEPLOY_GUIDE.md)
- **Настройка noteapp**: [NOTEAPP_INTEGRATION_GUIDE.md](NOTEAPP_INTEGRATION_GUIDE.md)
- **Troubleshooting**: [FIX_CONNECTION_ERROR.md](FIX_CONNECTION_ERROR.md)

---

## 🎯 Контрольный чеклист

- [ ] AI Integration backend запущен и доступен
- [ ] AI Integration frontend развернут и доступен
- [ ] Создан администратор в админке
- [ ] Создан клиент для noteapp
- [ ] API Key скопирован и настроен в noteapp
- [ ] noteapp подключен к AI Integration
- [ ] Нейросети синхронизированы
- [ ] Транскрибация аудио работает
- [ ] SSL/HTTPS настроен
- [ ] Backup БД настроен
- [ ] Мониторинг настроен (опционально)

---

## 🆘 Быстрая помощь

**Проблема**: `Connection to localhost:5432 refused`
```bash
# Решение: Создайте .env файл с DB_URL
echo "DB_URL=jdbc:postgresql://postgres:5432/ai_integration_db" >> .env
docker-compose restart ai-service
```

**Проблема**: Фронтенд не может подключиться к API
```bash
# Решение: Проверьте VITE_API_URL
echo "VITE_API_URL=https://your-domain.com" > frontend/.env
cd frontend && npm run build
```

**Проблема**: noteapp возвращает `401 Unauthorized`
```bash
# Решение: Проверьте API Key
docker exec noteapp printenv | grep AI_INTEGRATION_API_KEY
# Если пустой - обновите .env и перезапустите
```

---

**Нужна помощь?** Проверьте полные инструкции в файлах выше! 📖

