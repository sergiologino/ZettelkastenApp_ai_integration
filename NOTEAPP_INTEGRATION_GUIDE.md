# Настройка noteapp для работы с AI Integration Service на Timeweb

## Обзор

Эта инструкция покажет, как настроить noteapp для использования AI Integration Service, развернутого на сервере Timeweb.

---

## Шаг 1: Получите API Key из AI Integration Service

### 1.1. Войдите в админку AI Integration

Откройте админку в браузере:
```
https://your-ai-domain.timeweb.cloud
```

Войдите с логином и паролем администратора.

### 1.2. Создайте клиента для noteapp

1. Перейдите на вкладку **"Clients"** (Клиенты)
2. Нажмите **"Add Client"** (Добавить клиента)
3. Заполните форму:
   - **Name**: `noteapp`
   - **Description**: `Main note-taking application`
   - **Is Active**: ✅ Включено

4. Нажмите **"Create"**

### 1.3. Скопируйте API Key

После создания клиента вы увидите:
- **Client ID**: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- **API Key**: `sk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

**⚠️ ВАЖНО**: Скопируйте API Key немедленно! Он больше не будет показан в открытом виде.

Если потеряли - нажмите **"Regenerate Key"** для генерации нового.

---

## Шаг 2: Настройте переменные окружения noteapp

### Вариант А: Через `.env` файл (для Docker Compose)

Откройте или создайте файл `.env` в корне проекта noteapp:

```bash
cd /path/to/noteapp
nano .env
```

Добавьте следующие переменные:

```env
# ==========================================
# AI Integration Service Configuration
# ==========================================

# URL AI Integration Service на Timeweb
AI_INTEGRATION_BASE_URL=https://your-ai-domain.timeweb.cloud

# API Key из админки (из Шага 1.3)
AI_INTEGRATION_API_KEY=sk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Таймаут запросов (мс)
AI_INTEGRATION_TIMEOUT=30000

# Количество попыток при ошибке
AI_INTEGRATION_RETRY_ATTEMPTS=3

# ==========================================
# Автоматическая регистрация (опционально)
# ==========================================

# Если хотите автоматически зарегистрировать noteapp
# при первом запуске, раскомментируйте:

# AI_AUTO_REGISTRATION=true
# AI_ADMIN_USERNAME=admin
# AI_ADMIN_PASSWORD=your-admin-password
```

Сохраните файл (Ctrl+O, Enter, Ctrl+X в nano).

### Вариант Б: Через переменные окружения системы

Если запускаете noteapp вручную:

```bash
export AI_INTEGRATION_BASE_URL="https://your-ai-domain.timeweb.cloud"
export AI_INTEGRATION_API_KEY="sk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
export AI_INTEGRATION_TIMEOUT="30000"
export AI_INTEGRATION_RETRY_ATTEMPTS="3"

# Запустите noteapp
java -jar build/libs/noteapp-*.jar
```

### Вариант В: Через application.yml (не рекомендуется для секретов)

Откройте `src/main/resources/application.yml`:

```yaml
ai:
  integration:
    base-url: https://your-ai-domain.timeweb.cloud
    api-key: sk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # ⚠️ НЕ коммитьте в Git!
    timeout: 30000
    retry-attempts: 3
```

**⚠️ ВНИМАНИЕ**: Не коммитьте API ключи в Git! Используйте переменные окружения.

---

## Шаг 3: Обновите docker-compose.yml noteapp

Если используете Docker Compose, обновите файл:

```yaml
services:
  noteapp:
    image: your-noteapp-image
    container_name: noteapp
    environment:
      # ... существующие переменные
      
      # AI Integration
      AI_INTEGRATION_BASE_URL: ${AI_INTEGRATION_BASE_URL:-https://your-ai-domain.timeweb.cloud}
      AI_INTEGRATION_API_KEY: ${AI_INTEGRATION_API_KEY}
      AI_INTEGRATION_TIMEOUT: ${AI_INTEGRATION_TIMEOUT:-30000}
      AI_INTEGRATION_RETRY_ATTEMPTS: ${AI_INTEGRATION_RETRY_ATTEMPTS:-3}
    ports:
      - "8080:8080"
    networks:
      - noteapp-network
```

---

## Шаг 4: Перезапустите noteapp

### Если используется Docker Compose:

```bash
cd /path/to/noteapp
docker-compose down
docker-compose up -d

# Проверьте логи
docker logs noteapp -f
```

### Если запускается вручную:

```bash
# Остановите текущий процесс
# Запустите с новыми переменными
java -jar build/libs/noteapp-*.jar
```

---

## Шаг 5: Проверьте подключение

### 5.1. Проверка в логах

Откройте логи noteapp:

```bash
docker logs noteapp -f
```

Должны увидеть сообщения о подключении к AI Integration:

```
✅ [AI Integration] Подключение к сервису: https://your-ai-domain.timeweb.cloud
✅ [AI Integration] API Key настроен
✅ [AI Integration] Синхронизация нейросетей завершена: 5 сетей загружено
```

### 5.2. Проверка через API

Выполните тестовый запрос:

```bash
curl http://localhost:8080/api/neural-networks
```

Должен вернуть список доступных нейросетей:

```json
[
  {
    "id": "uuid",
    "name": "whisper-1",
    "displayName": "Whisper (OpenAI)",
    "provider": "openai",
    "networkType": "transcription",
    "isActive": true
  }
]
```

### 5.3. Проверка через фронтенд noteapp

1. Откройте noteapp в браузере
2. Попробуйте создать заметку с аудио
3. Аудио должно автоматически транскрибироваться
4. В логах должны появиться запросы к AI Integration

---

## Шаг 6: Настройте Nginx для noteapp (если нужно)

Если noteapp также на Timeweb и вы хотите использовать один домен:

```nginx
server {
    listen 80;
    server_name noteapp.your-domain.com;

    # Фронтенд noteapp
    location / {
        proxy_pass http://localhost:3000;  # Или путь к статике
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # API noteapp
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Увеличенные таймауты для AI запросов
        proxy_connect_timeout 90s;
        proxy_send_timeout 90s;
        proxy_read_timeout 90s;
    }

    # WebSocket для real-time уведомлений
    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

---

## Автоматическая регистрация (опционально)

Если хотите, чтобы noteapp автоматически регистрировался в AI Integration при первом запуске:

### 1. Включите профиль `ai-auto`

В `.env` noteapp:

```env
SPRING_PROFILES_ACTIVE=ai-auto

AI_AUTO_REGISTRATION=true
AI_INTEGRATION_BASE_URL=https://your-ai-domain.timeweb.cloud
AI_ADMIN_USERNAME=admin
AI_ADMIN_PASSWORD=your-admin-password
```

### 2. Перезапустите noteapp

```bash
docker-compose restart noteapp
```

### 3. Проверьте логи

```bash
docker logs noteapp -f
```

Должны увидеть:

```
🔄 [AI Auto-Registration] Начало автоматической регистрации...
✅ [AI Auto-Registration] Клиент успешно зарегистрирован
✅ [AI Auto-Registration] API Key сохранен в БД
✅ [AI Auto-Registration] Синхронизация нейросетей завершена
```

---

## Использование в коде noteapp

### Пример: Транскрибация аудио

```java
@Service
public class AudioTranscriptionService {
    
    @Autowired
    private NeuralNetworkService neuralNetworkService;
    
    public String transcribeAudio(File audioFile) {
        // Получаем доступную сеть для транскрибации
        NeuralNetwork network = neuralNetworkService
            .findBestAvailableNetwork("transcription");
        
        if (network == null) {
            throw new RuntimeException("No transcription networks available");
        }
        
        // Отправляем запрос через AI Integration
        String transcription = neuralNetworkService
            .transcribeAudio(network.getId(), audioFile);
        
        return transcription;
    }
}
```

### Пример: Генерация текста (GPT)

```java
@Service
public class AiChatService {
    
    @Autowired
    private NeuralNetworkService neuralNetworkService;
    
    public String generateResponse(String prompt) {
        // Получаем доступную GPT сеть
        NeuralNetwork network = neuralNetworkService
            .findBestAvailableNetwork("chat");
        
        if (network == null) {
            throw new RuntimeException("No chat networks available");
        }
        
        // Генерируем ответ
        String response = neuralNetworkService
            .generateText(network.getId(), prompt);
        
        return response;
    }
}
```

---

## Мониторинг и логирование

### 1. Логи запросов к AI Integration

В noteapp все запросы к AI Integration логируются:

```
[AI Integration] Отправка запроса: POST /api/ai/networks/{id}/transcribe
[AI Integration] Ответ получен: 200 OK, время: 2.5s
[AI Integration] Использовано токенов: 150
```

### 2. Просмотр логов в админке AI Integration

1. Откройте админку AI Integration
2. Перейдите на вкладку **"Logs"** (Логи)
3. Фильтруйте по клиенту `noteapp`
4. Смотрите статистику:
   - Количество запросов
   - Успешность
   - Использование токенов
   - Ошибки

### 3. Метрики Prometheus (если настроены)

```bash
# Метрики noteapp
curl http://localhost:8080/actuator/prometheus | grep ai_integration

# Метрики AI Integration
curl http://localhost:8091/actuator/prometheus
```

---

## Troubleshooting

### Проблема: noteapp не может подключиться к AI Integration

**Симптомы:**
```
Connection refused: connect to https://your-ai-domain.timeweb.cloud
```

**Причины и решения:**

1. **Неправильный URL**
   ```bash
   # Проверьте, что URL правильный
   curl https://your-ai-domain.timeweb.cloud/actuator/health
   ```

2. **Файрволл блокирует исходящие запросы**
   ```bash
   # Разрешите исходящие HTTPS запросы
   sudo ufw allow out 443/tcp
   ```

3. **AI Integration Service не запущен**
   ```bash
   # Проверьте статус
   docker ps | grep ai-integration
   docker logs ai-integration-service
   ```

### Проблема: 401 Unauthorized

**Причина**: Неправильный API Key

**Решение:**
1. Проверьте API Key в `.env` noteapp
2. Проверьте, что клиент активен в админке AI Integration
3. Regenerate API Key если потеряли оригинальный

### Проблема: 429 Too Many Requests

**Причина**: Превышен лимит запросов

**Решение:**
1. В админке AI Integration увеличьте лимиты для noteapp
2. Или подождите, пока лимит сбросится (обычно каждый день/месяц)

### Проблема: Транскрибация не работает

**Проверьте:**

1. **Синхронизация нейросетей**
   ```bash
   curl http://localhost:8080/api/neural-networks
   # Должны быть сети с networkType: "transcription"
   ```

2. **Логи noteapp**
   ```bash
   docker logs noteapp | grep transcription
   ```

3. **Формат аудио**
   - Поддерживаемые форматы: mp3, wav, ogg, m4a
   - Максимальный размер: 25 MB (для OpenAI Whisper)

---

## Обновление конфигурации

### Изменение AI Integration URL

```bash
# 1. Обновите .env
nano .env
# Измените AI_INTEGRATION_BASE_URL

# 2. Перезапустите noteapp
docker-compose restart noteapp
```

### Смена API Key

```bash
# 1. В админке AI Integration regenerate key
# 2. Скопируйте новый key
# 3. Обновите .env noteapp
nano .env
# Измените AI_INTEGRATION_API_KEY

# 4. Перезапустите
docker-compose restart noteapp
```

### Добавление новых нейросетей

Новые нейросети автоматически синхронизируются:
- При запуске noteapp
- Каждые 24 часа автоматически
- Или принудительно через API: `POST /api/neural-networks/sync`

---

## Безопасность

### ⚠️ ВАЖНО для продакшена:

1. **НЕ храните API ключи в Git**
   ```bash
   # Добавьте в .gitignore
   echo ".env" >> .gitignore
   echo "application-ai.yml" >> .gitignore
   ```

2. **Используйте HTTPS**
   - AI Integration должен быть доступен только по HTTPS
   - noteapp → AI Integration: только HTTPS

3. **Ограничьте сетевой доступ**
   - В админке AI Integration настройте разрешенные IP для noteapp
   - Используйте VPN или приватную сеть между серверами

4. **Регулярно обновляйте ключи**
   - Меняйте API ключи каждые 90 дней
   - Используйте разные ключи для разных окружений (dev, staging, prod)

---

## Полезные ссылки

- **AI Integration Admin**: `https://your-ai-domain.timeweb.cloud`
- **AI Integration API Docs**: `https://your-ai-domain.timeweb.cloud/swagger-ui/`
- **noteapp**: `https://noteapp.your-domain.com`

---

## Следующие шаги

1. ✅ Настроили подключение noteapp к AI Integration
2. ✅ Проверили работу транскрибации
3. ⬜ Настройте мониторинг (Prometheus + Grafana)
4. ⬜ Настройте backup для обеих БД
5. ⬜ Добавьте больше нейросетей в AI Integration
6. ⬜ Настройте лимиты использования
7. ⬜ Настройте алерты для критичных событий

Поздравляем! noteapp теперь интегрирован с AI Integration Service! 🎉

