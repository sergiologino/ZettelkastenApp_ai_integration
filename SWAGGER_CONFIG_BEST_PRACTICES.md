# 🎯 Swagger Configuration - Best Practices

## Проблема
Hardcode URL в коде плохая практика - один и тот же код должен работать на разных окружениях (development, staging, production).

## ✅ Решение
Используем переменные окружения для конфигурации Swagger серверов.

---

## 📋 Что изменено

### 1. `OpenApiConfig.java` - динамическая конфигурация

```java
@Bean
public OpenAPI customOpenAPI() {
    OpenAPI openAPI = new OpenAPI();
    
    if (serverUrl != null && !serverUrl.isEmpty()) {
        // Production: используем переменную окружения
        Server server = new Server();
        server.setUrl(serverUrl);
        server.setDescription(serverDescription);
        openAPI.servers(List.of(server));
    } else {
        // Development: используем текущий домен (относительный URL)
        Server server = new Server();
        server.setUrl(""); // Пустая строка = текущий домен
        server.setDescription("Current server");
        openAPI.servers(List.of(server));
    }
    
    return openAPI;
}
```

### 2. `application.yml` - настройка переменных

```yaml
# Swagger Configuration
swagger:
  server:
    url: ${SWAGGER_SERVER_URL:}
    description: ${SWAGGER_SERVER_DESCRIPTION:Current server}
```

### 3. `docker-compose.yml` - передача переменных в контейнер

```yaml
environment:
  # Swagger
  SWAGGER_SERVER_URL: ${SWAGGER_SERVER_URL:-}
  SWAGGER_SERVER_DESCRIPTION: ${SWAGGER_SERVER_DESCRIPTION:-Current server}
```

---

## 🚀 Как использовать

### Вариант 1: Локальная разработка (без Docker)

Просто запускайте приложение - Swagger будет использовать текущий домен:

```bash
./gradlew bootRun
```

Swagger UI: `http://localhost:8091/swagger-ui/index.html`

### Вариант 2: Локальная разработка (с Docker)

Создайте файл `.env`:

```bash
cp .env.example .env
```

Запустите:

```bash
docker-compose up -d
```

Swagger будет использовать `http://localhost:8091`

### Вариант 3: Production (Timeweb или другой хостинг)

Создайте файл `.env` на сервере:

```bash
# .env
SWAGGER_SERVER_URL=https://sergiologino-zettelkastenapp-ai-integration-bce3.twc1.net
SWAGGER_SERVER_DESCRIPTION=Production Server

# ... остальные переменные ...
```

Запустите:

```bash
docker-compose up -d
```

Swagger будет использовать production URL.

### Вариант 4: Деплой на Timeweb через переменные окружения

Если вы разворачиваете через Timeweb UI, добавьте переменные окружения:

| Variable | Value |
|----------|-------|
| `SWAGGER_SERVER_URL` | `https://sergiologino-zettelkastenapp-ai-integration-bce3.twc1.net` |
| `SWAGGER_SERVER_DESCRIPTION` | `Production Server` |

---

## 🎨 Преимущества этого подхода

### ✅ Один код для всех окружений
- Development: автоматически использует `localhost`
- Staging: использует staging URL из переменных
- Production: использует production URL из переменных

### ✅ Безопасность
- Нет hardcode URLs в коде
- Все конфигурируется через переменные окружения
- Легко управлять доступами

### ✅ Гибкость
- Пустое значение `SWAGGER_SERVER_URL` → текущий домен
- Указанное значение → конкретный URL
- Можно легко переключаться между окружениями

### ✅ Best Practices
- [12 Factor App](https://12factor.net/config) - конфигурация через окружение
- Spring Boot Best Practices - использование `@Value` и `application.yml`
- Docker Best Practices - переменные окружения через `.env` файл

---

## 📝 Проверка

После деплоя проверьте логи:

```bash
docker-compose logs ai-service | grep "Swagger"
```

Вы должны увидеть:

### Локально (без SWAGGER_SERVER_URL):
```
📝 Swagger using current domain (relative URL)
```

### Production (с SWAGGER_SERVER_URL):
```
📝 Swagger server configured: https://sergiologino-zettelkastenapp-ai-integration-bce3.twc1.net
```

---

## 🔧 Troubleshooting

### Проблема: Swagger показывает неправильный URL

**Решение:**
1. Проверьте переменные окружения:
```bash
docker-compose exec ai-service env | grep SWAGGER
```

2. Перезапустите контейнер:
```bash
docker-compose restart ai-service
```

### Проблема: CORS ошибки в Swagger

**Решение:**
1. Убедитесь что `SWAGGER_SERVER_URL` соответствует домену в браузере
2. Проверьте CORS настройки в `SecurityConfig.java`

---

## 📚 Ссылки

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [Docker Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [12 Factor App - Config](https://12factor.net/config)

