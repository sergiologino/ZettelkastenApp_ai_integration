# Деплой фронтенда AI Integration Service на Timeweb

## Обзор

Фронтенд - это административная панель для управления AI Integration Service:
- Управление нейросетями
- Управление клиентскими приложениями (noteapp)
- Просмотр логов запросов
- Статистика использования

---

## Вариант 1: Деплой как статические файлы (Nginx)

### 1.1. Сборка фронтенда локально

```bash
cd /path/to/noteapp-ai-integration/frontend

# Установите зависимости (если еще не установлены)
npm install

# Создайте .env файл для production
cat > .env << 'EOF'
VITE_API_URL=https://your-domain.com
EOF

# Замените your-domain.com на ваш домен или IP Timeweb
# Например:
# VITE_API_URL=https://ai-integration.timeweb.cloud
# или
# VITE_API_URL=http://YOUR_SERVER_IP:8091

# Соберите production версию
npm run build
```

Результат будет в папке `dist/`.

### 1.2. Загрузка на сервер

```bash
# Скопируйте собранные файлы на сервер
scp -r dist/* user@your-server:/var/www/ai-integration-admin/

# Или через rsync
rsync -avz dist/ user@your-server:/var/www/ai-integration-admin/
```

### 1.3. Настройка Nginx

Создайте конфигурацию Nginx:

```bash
sudo nano /etc/nginx/sites-available/ai-integration-admin
```

Вставьте следующую конфигурацию:

```nginx
server {
    listen 80;
    server_name ai-admin.your-domain.com;  # Измените на свой домен

    # Корневая директория с собранным фронтендом
    root /var/www/ai-integration-admin;
    index index.html;

    # SPA routing - все запросы перенаправляем на index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API запросы проксируем на backend
    location /api/ {
        proxy_pass http://localhost:8091/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        
        # Таймауты для AI запросов
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Actuator health check
    location /actuator/ {
        proxy_pass http://localhost:8091/actuator/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Кеширование статических файлов
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Логи
    access_log /var/log/nginx/ai-admin-access.log;
    error_log /var/log/nginx/ai-admin-error.log;
}
```

Активируйте конфигурацию:

```bash
# Создайте симлинк
sudo ln -s /etc/nginx/sites-available/ai-integration-admin /etc/nginx/sites-enabled/

# Проверьте конфигурацию
sudo nginx -t

# Перезагрузите Nginx
sudo systemctl reload nginx
```

### 1.4. Настройка SSL (Let's Encrypt)

```bash
# Установите certbot (если еще не установлен)
sudo apt-get update
sudo apt-get install certbot python3-certbot-nginx

# Получите SSL сертификат
sudo certbot --nginx -d ai-admin.your-domain.com

# Certbot автоматически обновит конфигурацию Nginx для HTTPS
```

---

## Вариант 2: Деплой с помощью Docker

### 2.1. Создайте Dockerfile для фронтенда

Создайте `frontend/Dockerfile`:

```dockerfile
# Multi-stage build для фронтенда
FROM node:20-alpine AS build

WORKDIR /app

# Копируем package files
COPY package*.json ./

# Устанавливаем зависимости
RUN npm ci

# Копируем исходники
COPY . .

# Собираем production версию
RUN npm run build

# Production stage - nginx для статики
FROM nginx:alpine

# Копируем собранные файлы
COPY --from=build /app/dist /usr/share/nginx/html

# Копируем кастомную конфигурацию nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 2.2. Создайте nginx.conf для фронтенда

Создайте `frontend/nginx.conf`:

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Кеширование
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Gzip сжатие
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
```

### 2.3. Обновите docker-compose.yml

Добавьте сервис для фронтенда:

```yaml
services:
  # ... существующие сервисы (postgres, ai-service)

  # AI Integration Admin Frontend
  ai-admin-frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: ai-admin-frontend
    environment:
      - VITE_API_URL=http://ai-service:8091
    ports:
      - "3001:80"  # Порт для доступа к фронтенду
    networks:
      - ai-integration-network
    depends_on:
      - ai-service
    restart: unless-stopped
```

### 2.4. Запустите с фронтендом

```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d

# Проверьте логи
docker logs ai-admin-frontend -f
```

Теперь фронтенд доступен на `http://your-server:3001`

---

## Вариант 3: Встроить в один Nginx (рекомендуется для Timeweb)

Если у вас один домен и вы хотите все под одним Nginx:

```nginx
server {
    listen 80;
    server_name ai-integration.your-domain.com;

    # Админка на корневом пути
    location / {
        root /var/www/ai-integration-admin;
        try_files $uri $uri/ /index.html;
        index index.html;
    }

    # API на /api
    location /api/ {
        proxy_pass http://localhost:8091/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Таймауты
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health check
    location /actuator/ {
        proxy_pass http://localhost:8091/actuator/;
    }

    # Swagger UI (опционально)
    location /swagger-ui/ {
        proxy_pass http://localhost:8091/swagger-ui/;
    }

    # API docs (опционально)
    location /v3/api-docs {
        proxy_pass http://localhost:8091/v3/api-docs;
    }
}
```

---

## Проверка деплоя

### 1. Проверьте, что backend работает

```bash
curl http://localhost:8091/actuator/health
# Ожидается: {"status":"UP"}
```

### 2. Проверьте, что фронтенд доступен

Откройте в браузере:
- **Локально**: `http://localhost:3001` (Docker) или `http://your-domain.com`
- **Timeweb**: `https://your-domain.com`

### 3. Проверьте логи

```bash
# Логи backend
docker logs ai-integration-service -f

# Логи frontend (если Docker)
docker logs ai-admin-frontend -f

# Логи Nginx
sudo tail -f /var/log/nginx/ai-admin-access.log
sudo tail -f /var/log/nginx/ai-admin-error.log
```

---

## Первый вход в админку

### 1. Создайте администратора

Если БД пустая, запустите seed данные или создайте админа вручную:

```bash
# Войдите в контейнер БД
docker exec -it ai-integration-db psql -U postgres -d ai_integration_db

# Создайте администратора (пароль будет захеширован при первом входе)
INSERT INTO admins (username, password, created_at, updated_at) 
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye7RGwGd3Qm3YLLvdVaH7G0YzM3WM6OKq', NOW(), NOW());
-- Пароль: admin (ОБЯЗАТЕЛЬНО смените после первого входа!)
```

Или используйте API:

```bash
curl -X POST http://localhost:8091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "your-secure-password"
  }'
```

### 2. Войдите в админку

Откройте `https://your-domain.com` и введите:
- **Username**: `admin`
- **Password**: ваш пароль

---

## Настройка Timeweb специфики

### 1. Используйте Timeweb домен

Если у вас домен на Timeweb:

```env
# В frontend/.env
VITE_API_URL=https://ai-integration.timeweb.cloud
```

### 2. Настройте файрволл

```bash
# Разрешите HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Закройте прямой доступ к backend (если не нужен)
sudo ufw deny 8091/tcp
```

### 3. Используйте переменные окружения Timeweb

В панели Timeweb добавьте переменные:
```
AI_INTEGRATION_BASE_URL=https://ai-integration.timeweb.cloud
DB_URL=jdbc:postgresql://postgres:5432/ai_integration_db
DB_USERNAME=postgres
DB_PASSWORD=your-password
```

---

## Обновление фронтенда

### Если используется Nginx + статика:

```bash
# 1. Локально пересоберите
cd frontend
npm run build

# 2. Загрузите на сервер
rsync -avz dist/ user@server:/var/www/ai-integration-admin/

# 3. Очистите кеш браузера
```

### Если используется Docker:

```bash
# На сервере
cd noteapp-ai-integration
git pull
docker-compose build ai-admin-frontend --no-cache
docker-compose up -d ai-admin-frontend
```

---

## Troubleshooting

### Проблема: Фронтенд не может подключиться к API

**Проверьте:**
1. Правильно ли указан `VITE_API_URL` в `.env`
2. Работает ли backend: `curl http://localhost:8091/actuator/health`
3. Открыт ли порт 8091 или настроен Nginx прокси

**Решение:**
```bash
# В браузере откройте DevTools (F12) -> Console
# Должны быть запросы к /api/
# Если 404 - проверьте Nginx конфигурацию
# Если CORS ошибки - добавьте CORS в backend
```

### Проблема: После сборки фронтенд пустой (белый экран)

**Причина**: Неправильный `base` path в Vite

**Решение**: Обновите `vite.config.ts`:

```typescript
export default defineConfig({
  plugins: [react()],
  base: '/',  // Или '/admin/' если в подпапке
})
```

### Проблема: 401 ошибки при входе

**Причина**: Неправильный JWT secret или не создан администратор

**Решение**: Проверьте переменные окружения backend и создайте админа

---

## Следующие шаги

1. ✅ Настройте SSL/HTTPS
2. ✅ Создайте администратора
3. ✅ Создайте клиента для noteapp в админке
4. ✅ Скопируйте API key и настройте noteapp (см. следующий раздел)
5. ✅ Добавьте нейросети в AI Integration
6. ✅ Настройте мониторинг (опционально)

Продолжайте читать **NOTEAPP_INTEGRATION_GUIDE.md** для настройки noteapp.

