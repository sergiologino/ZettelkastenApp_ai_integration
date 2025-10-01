# Руководство по установке и запуску фронтенда AI Integration Service

## 🚀 Быстрый старт

### 1. Установка зависимостей

```bash
cd frontend
npm install
```

### 2. Настройка переменных окружения

Создайте файл `.env` в папке `frontend`:

```bash
VITE_API_URL=http://localhost:8091
```

Для продакшена измените на URL вашего AI-сервиса.

### 3. Запуск dev сервера

```bash
npm run dev
```

Откроется на: **http://localhost:5173**

## 🔐 Первый вход

1. Убедитесь, что бекенд запущен на `http://localhost:8091`
2. Откройте Swagger UI: `http://localhost:8091/swagger-ui.html`
3. Создайте администратора:
   - Endpoint: `POST /api/auth/register`
   - Body: `{"username": "admin", "password": "your_password"}`
4. Войдите в админ-панель с этими credentials

## 📦 Сборка для продакшена

```bash
npm run build
```

Файлы будут в папке `dist/`

## ✨ Возможности админ-панели

### 📊 Статистика
- Общее количество запросов
- Успешные/неуспешные запросы
- Использованные токены
- Распределение по нейросетям и клиентам

### 🧠 Управление нейросетями
- Добавление OpenAI, Yandex GPT, Claude, Mistral, GigaChat, Whisper
- Настройка приоритетов и лимитов
- Активация/деактивация
- Безопасное хранение API ключей

### 🔑 Управление клиентами
- Создание клиентских приложений
- Генерация API ключей
- Копирование ключей одним кликом
- Регенерация ключей

### 📋 Логи запросов
- Просмотр всех запросов с пагинацией
- Детали: промпт, ответ, ошибки, токены
- Фильтрация по статусу, клиенту, сети

## 🛠 Технологии

- React 18 + TypeScript
- Vite
- Tailwind CSS
- Fetch API

## 📝 Структура

```
frontend/
├── src/
│   ├── components/
│   │   ├── Login.tsx          # Страница входа
│   │   ├── Dashboard.tsx      # Главная панель
│   │   ├── StatsPanel.tsx     # Статистика
│   │   ├── NetworksManager.tsx # Нейросети
│   │   ├── ClientsManager.tsx  # Клиенты
│   │   └── LogsViewer.tsx      # Логи
│   ├── api.ts                  # API клиент
│   ├── types.ts                # TypeScript типы
│   └── App.tsx                 # Главный компонент
```

## 🚢 Deployment

### NGINX

```nginx
server {
    listen 80;
    server_name ai-admin.yourdomain.com;
    
    root /path/to/dist;
    index index.html;
    
    location / {
        try_files $uri /index.html;
    }
}
```

### Docker

```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 🔧 Разработка

```bash
# Установка
npm install

# Запуск
npm run dev

# Сборка
npm run build

# Preview
npm run preview
```

## ⚠️ Важно

- Не забудьте создать файл `.env` с `VITE_API_URL`
- В продакшене используйте HTTPS
- Храните JWT токены безопасно
- Регулярно обновляйте зависимости

## 🆘 Troubleshooting

### Ошибка "Не авторизован"
- Проверьте, что бекенд запущен
- Проверьте правильность URL в `.env`
- Попробуйте выйти и войти заново

### Не отображаются данные
- Проверьте CORS настройки на бекенде
- Откройте DevTools → Network и проверьте запросы
- Убедитесь, что JWT токен валиден

### Ошибки при сборке
- Удалите `node_modules` и `package-lock.json`
- Запустите `npm install` заново
- Проверьте версию Node.js (требуется >= 16)

