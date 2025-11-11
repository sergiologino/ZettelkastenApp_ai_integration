# 🚀 Инструкции по деплою изменений

## 📝 Что изменилось

**Файл:** `src/main/java/com/example/integration/service/AiOrchestrationService.java`

**Изменения:**
- Улучшено логирование в методе `getAvailableNetworksForClient()`
- Все логи переведены с `log.debug()` на `log.info()` для видимости
- Добавлены детальные логи для диагностики проблемы с пустым списком нейросетей

---

## 🔨 Сборка проекта

### **Вариант 1: Gradle (рекомендуется)**

```bash
cd noteapp-ai-integration

# Очистка и сборка
./gradlew clean build

# Или на Windows
gradlew.bat clean build
```

**Результат:** JAR файл будет в `build/libs/noteapp-ai-integration-{version}.jar`

---

### **Вариант 2: Maven**

```bash
cd noteapp-ai-integration

# Очистка и сборка
mvn clean package

# Пропустить тесты (если нужно быстро)
mvn clean package -DskipTests
```

**Результат:** JAR файл будет в `target/noteapp-ai-integration-{version}.jar`

---

## 🐳 Деплой на Timeweb через Docker

### **Шаг 1: Загрузите новый код на сервер**

```bash
# С локального компьютера
scp -r noteapp-ai-integration root@your-timeweb-server:/path/to/projects/

# Или через git
ssh root@your-timeweb-server
cd /path/to/projects/noteapp-ai-integration
git pull origin main
```

---

### **Шаг 2: Пересоберите Docker образ**

```bash
# Подключитесь к серверу
ssh root@your-timeweb-server

# Перейдите в директорию проекта
cd /path/to/projects/noteapp-ai-integration

# Пересоберите образ
docker-compose build ai-integration-service

# Или через docker build напрямую
docker build -t ai-integration-service:latest .
```

---

### **Шаг 3: Перезапустите контейнер**

```bash
# Остановите старый контейнер
docker-compose stop ai-integration-service

# Запустите новый
docker-compose up -d ai-integration-service

# Или используйте restart (пересоздаст контейнер)
docker-compose up -d --force-recreate ai-integration-service
```

---

### **Шаг 4: Проверьте логи**

```bash
# Смотрим логи в реальном времени
docker logs ai-integration-service -f

# Или через docker-compose
docker-compose logs -f ai-integration-service
```

**Должны увидеть при старте:**
```
🔧 SecurityConfig ЗАГРУЖЕН!
✅ API Key фильтр будет подключен!
✅ SecurityFilterChain настроен - API Key фильтр включен
Tomcat started on port 8091 (http) with context path '/'
Started NoteappAiIntegrationApplication in X.XXX seconds
```

---

## 🧪 Тестирование

### **1. Проверьте health endpoint:**

```bash
curl https://sergiologino-zettelkastenapp-ai-integration-bce3.twc1.net/actuator/health
```

**Ожидаемый результат:**
```json
{
  "status": "UP"
}
```

---

### **2. Запустите синхронизацию из frontend:**

- Откройте: `https://altanote.ru`
- Войдите в систему
- Профиль → "Доступные нейросети"
- Нажмите кнопку "Синхронизация" (🔄)

---

### **3. Проверьте логи AI Integration Service:**

```bash
docker logs ai-integration-service -f
```

**ТЕПЕРЬ должны увидеть:**

```
🔵 [ApiKeyAuthFilter] Обработка запроса к /networks/available
🔍 [ApiKeyAuthFilter] X-API-Key header: присутствует
🔍 [ApiKeyAuthFilter] API Key preview: aikey_02fceb86f20d43d8...
✅ [ApiKeyAuthFilter] Клиент найден: noteapp (ID: 864765f7-...)

🔵 [AiController] ===== ЗАПРОС /api/ai/networks/available =====
🔍 [AiOrchestrationService] Получаем доступные нейросети для клиента: noteapp
🔍 [AiOrchestrationService] Найдено 2 доступов для клиента

🔍 [AiOrchestrationService] Ищем нейросеть по ID: a1b2c3d4-e5f6-...  ⬅️ НОВЫЙ ЛОГ!
✅ [AiOrchestrationService] Найдена активная нейросеть: OpenAI Whisper (тип: transcription, provider: openai)  ⬅️ НОВЫЙ ЛОГ!
   📊 Лимиты: daily=null, monthly=null, hasLimits=false  ⬅️ НОВЫЙ ЛОГ!

🔍 [AiOrchestrationService] Ищем нейросеть по ID: f6e5d4c3-b2a1-...  ⬅️ НОВЫЙ ЛОГ!
✅ [AiOrchestrationService] Найдена активная нейросеть: Yandex GPT Pro (тип: chat, provider: yandex)  ⬅️ НОВЫЙ ЛОГ!
   📊 Лимиты: daily=null, monthly=null, hasLimits=false  ⬅️ НОВЫЙ ЛОГ!

✅ [AiOrchestrationService] Возвращаем 2 доступных нейросетей для клиента noteapp  ⬅️ 2, НЕ 0!
  - OpenAI Whisper (тип: transcription, provider: openai, приоритет: 10)  ⬅️ НОВЫЙ ЛОГ!
  - Yandex GPT Pro (тип: chat, provider: yandex, приоритет: 5)  ⬅️ НОВЫЙ ЛОГ!

✅ [AiController] Получено 2 доступных нейросетей для клиента noteapp  ⬅️ 2, НЕ 0!
```

**Если всё еще видите `Возвращаем 0 доступных нейросетей`, то:**
- Новые логи (`Ищем нейросеть по ID...`) покажут, ГДЕ именно происходит фильтрация
- Нейросети либо не найдены в БД, либо неактивны

---

### **4. Проверьте логи noteapp backend:**

```bash
docker logs noteapp-backend -f
```

**Должно быть:**

```
🔄 [NeuralNetworkService] ===== НАЧАЛО СИНХРОНИЗАЦИИ НЕЙРОСЕТЕЙ =====
✅ [AiIntegrationService] Получено 2 нейросетей из AI-сервиса ✅  ⬅️ 2, НЕ 0!
➕ Добавлена новая нейросеть: OpenAI Whisper
➕ Добавлена новая нейросеть: Yandex GPT Pro
✅ Синхронизировано: 2/2
```

---

### **5. Проверьте БД noteapp:**

```sql
SELECT 
    id, 
    name, 
    display_name, 
    provider, 
    network_type, 
    is_active,
    priority
FROM neural_networks
ORDER BY priority DESC;
```

**Должно вывести:**

```
id                                   | name           | display_name   | provider | network_type   | is_active | priority
-------------------------------------|----------------|----------------|----------|----------------|-----------|----------
a1b2c3d4-e5f6-...                    | openai-whisper | OpenAI Whisper | openai   | transcription  | true      | 10
f6e5d4c3-b2a1-...                    | yandex-gpt-pro | Yandex GPT Pro | yandex   | chat           | true      | 5
```

---

## ⚠️ Если логи всё еще показывают 0 нейросетей

Новые логи помогут понять проблему:

### **Случай 1: Нейросети не найдены в БД**

```
⚠️ [AiOrchestrationService] Нейросеть с ID a1b2c3d4-... не найдена в БД
```

**Причина:** UUID нейросетей в таблице `client_network_access` не соответствуют UUID в таблице `neural_networks`.

**Решение:**
```sql
-- Проверьте UUID нейросетей
SELECT id, name, display_name FROM neural_networks;

-- Проверьте UUID доступов
SELECT 
    cna.id, 
    ca.name as client_name,
    nn.name as network_name,
    cna.neural_network_id
FROM client_network_access cna
JOIN client_applications ca ON cna.client_application_id = ca.id
LEFT JOIN neural_networks nn ON cna.neural_network_id = nn.id
WHERE ca.name = 'noteapp';

-- Если nn.name NULL, значит UUID не совпадают!
```

---

### **Случай 2: Нейросети неактивны**

```
⚠️ [AiOrchestrationService] Нейросеть OpenAI Whisper неактивна (is_active=false)
```

**Причина:** Поле `is_active = false` в таблице `neural_networks`.

**Решение:**
```sql
UPDATE neural_networks SET is_active = true;
```

---

### **Случай 3: Нейросети найдены и активны, но всё еще 0**

Если логи показывают:
```
✅ Найдена активная нейросеть: OpenAI Whisper
✅ Найдена активная нейросеть: Yandex GPT Pro
✅ Возвращаем 0 доступных нейросетей  ❌
```

**Проблема:** Фильтр `.filter(dto -> dto != null)` отбрасывает все DTO.

**Проверьте:** Метод `convertToAvailableNetworkDTO()` возможно возвращает `null`.

---

## 📋 Checklist

- [ ] ✅ Код изменён (добавлено логирование)
- [ ] 🔨 Проект пересобран (`gradle build` / `mvn package`)
- [ ] 📦 JAR файл загружен на сервер
- [ ] 🐳 Docker образ пересобран (`docker-compose build`)
- [ ] 🔄 Контейнер перезапущен (`docker-compose up -d --force-recreate`)
- [ ] 🔍 Логи проверены (новые логи видны)
- [ ] 🧪 Синхронизация запущена
- [ ] ✅ Нейросети появились в БД noteapp

---

## 🆘 Если проблемы

1. **Контейнер не запускается:**
   ```bash
   docker logs ai-integration-service
   # Ищите ошибки Java, Spring Boot
   ```

2. **Порт уже занят:**
   ```bash
   docker ps | grep 8091
   docker stop <container_id>
   ```

3. **Изменения не применились:**
   ```bash
   # Проверьте, что пересобрали образ
   docker images | grep ai-integration
   
   # Убедитесь что пересоздали контейнер
   docker-compose up -d --force-recreate ai-integration-service
   ```

4. **БД недоступна:**
   ```bash
   docker logs ai-integration-service | grep "Could not open JDBC"
   # Проверьте настройки БД в docker-compose.yml
   ```

---

**После деплоя новые логи покажут, в чём реальная проблема! 🔍**

