# Multi-stage build для фронтенда AI Integration Admin

# ============================================
# Build stage
# ============================================
FROM node:20-alpine AS build

WORKDIR /app

# ARG для API URL (передается при сборке)
ARG VITE_API_URL=http://localhost:8091

# Копируем package files
COPY package*.json ./

# Устанавливаем зависимости
RUN npm ci --only=production

# Копируем исходники
COPY . .

# Создаем .env с API URL
RUN echo "VITE_API_URL=${VITE_API_URL}" > .env

# Собираем production версию
RUN npm run build

# ============================================
# Production stage - nginx для отдачи статики
# ============================================
FROM nginx:alpine

# Устанавливаем curl для healthcheck
RUN apk add --no-cache curl

# Копируем собранные файлы из build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Копируем кастомную конфигурацию nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Открываем порт
EXPOSE 80

# Запускаем nginx
CMD ["nginx", "-g", "daemon off;"]

