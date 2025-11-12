# 🚀 Интеграция Qwen и DeepSeek в AI Integration Service

## 📊 Обзор

Добавлена поддержка китайских AI моделей **Qwen** (Alibaba Cloud) и **DeepSeek** в AI Integration Service.

### **Почему Qwen и DeepSeek?**

1. **💰 Низкая стоимость:**
   - DeepSeek: ~$0.0014 за 1M входных токенов (в 10-20 раз дешевле GPT-4)
   - Qwen-Turbo: ~$0.002 за 1K токенов (дешевле GPT-3.5)

2. **🚀 Высокая производительность:**
   - DeepSeek-V3: 671B параметров, MoE архитектура (декабрь 2024)
   - Qwen2.5-72B: Последняя версия с открытыми весами

3. **🌏 Поддержка китайского языка:**
   - Отличное качество для китайского и английского
   - Хорошая поддержка русского языка

4. **🔧 OpenAI-совместимый API:**
   - Простая интеграция
   - Быстрая миграция с OpenAI

---

## 🗂️ Добавленные модели

### **Qwen Models (Alibaba Cloud)**

| Модель | Название | Стоимость | Описание | Priority |
|--------|----------|-----------|----------|----------|
| `qwen-turbo` | Qwen-Turbo (Fast & Cheap) | ~$0.002/1K tokens | Быстрая и дешевая | 30 |
| `qwen-plus` | Qwen-Plus (Balanced) | ~$0.008/1K tokens | Сбалансированная | 25 |
| `qwen-max` | Qwen-Max (Most Powerful) | ~$0.02/1K tokens | Самая мощная | 20 |
| `qwen2.5-72b-instruct` | Qwen2.5-72B-Instruct (Latest) | ~$0.009/1K tokens | Последняя версия | 18 |

**API Endpoint:** `https://dashscope.aliyuncs.com/api/v1`

**Документация:** https://help.aliyun.com/zh/dashscope/developer-reference/api-details

---

### **DeepSeek Models**

| Модель | Название | Стоимость | Описание | Priority |
|--------|----------|-----------|----------|----------|
| `deepseek-chat` | DeepSeek-Chat (General) | ~$0.0014/1M input tokens | Универсальная | 28 |
| `deepseek-coder` | DeepSeek-Coder (Code Specialist) | ~$0.0014/1M input tokens | Для кода | 26 |
| `deepseek-v3` | DeepSeek-V3 (Latest Flagship) | ~$0.0014/1M input tokens | Последняя (дек 2024) | 16 |

**API Endpoint:** `https://api.deepseek.com/v1`

**Документация:** https://platform.deepseek.com/api-docs/

**Особенности DeepSeek-V3:**
- 671B параметров (Mixture of Experts)
- Релиз: декабрь 2024
- Конкурирует с GPT-4 и Claude 3.5 Sonnet
- Цена в 10-20 раз ниже GPT-4

---

## 📁 Добавленные файлы

### **1. Миграция БД**

**Файл:** `src/main/resources/db/migration/V007__add_qwen_deepseek_models.sql`

**Содержимое:**
- 4 модели Qwen (turbo, plus, max, 2.5-72b)
- 3 модели DeepSeek (chat, coder, v3)
- Лимиты для free_user (30 запросов/день)
- Лимиты для paid_user (500 запросов/месяц)

---

### **2. Java Клиенты**

#### **QwenClient.java**

**Путь:** `src/main/java/com/example/integration/client/QwenClient.java`

**Особенности:**
- OpenAI-совместимый API
- Bearer token аутентификация
- Endpoint: `/v1/chat/completions`
- Логирование запросов и ответов
- Проверка наличия API ключа

**Пример использования:**
```java
// API ключ: DashScope API Key (Alibaba Cloud)
// Получить: https://dashscope.console.aliyun.com/
```

---

#### **DeepSeekClient.java**

**Путь:** `src/main/java/com/example/integration/client/DeepSeekClient.java`

**Особенности:**
- OpenAI-совместимый API
- Bearer token аутентификация
- Endpoint: `/v1/chat/completions`
- Поддержка DeepSeek-V3 (MoE модель)
- Детальное логирование структуры ответа

**Пример использования:**
```java
// API ключ: DeepSeek API Key
// Получить: https://platform.deepseek.com/
```

---

### **3. Обновление NeuralClientFactory**

**Файл:** `src/main/java/com/example/integration/client/NeuralClientFactory.java`

**Изменения:**
```java
// Добавлены поля:
private final QwenClient qwenClient;
private final DeepSeekClient deepSeekClient;

// Обновлен switch:
case "qwen" -> qwenClient;
case "deepseek" -> deepSeekClient;
```

---

## 🔧 Настройка

### **Шаг 1: Применить миграцию**

Миграция применится автоматически при запуске сервиса.

**Проверка:**
```bash
docker exec -it ai-integration-service sh
psql -U ai_user -d ai_integration_db -c "SELECT name, display_name, provider FROM neural_networks WHERE provider IN ('qwen', 'deepseek');"
```

**Ожидаемый результат:**
```
        name          |         display_name          | provider  
----------------------+-------------------------------+-----------
 qwen-turbo           | Qwen-Turbo (Fast & Cheap)    | qwen
 qwen-plus            | Qwen-Plus (Balanced)         | qwen
 qwen-max             | Qwen-Max (Most Powerful)     | qwen
 qwen2.5-72b-instruct | Qwen2.5-72B-Instruct (Latest)| qwen
 deepseek-chat        | DeepSeek-Chat (General)      | deepseek
 deepseek-coder       | DeepSeek-Coder (Code Spec.)  | deepseek
 deepseek-v3          | DeepSeek-V3 (Latest Flagship)| deepseek
(7 rows)
```

---

### **Шаг 2: Получить API ключи**

#### **Qwen (Alibaba Cloud)**

1. Регистрация: https://account.aliyun.com/
2. Консоль DashScope: https://dashscope.console.aliyun.com/
3. Создать API Key
4. Скопировать ключ (формат: `sk-...`)

**Цены:**
- Qwen-Turbo: ¥0.008/1K tokens (~$0.0011)
- Qwen-Plus: ¥0.04/1K tokens (~$0.0055)
- Qwen-Max: ¥0.12/1K tokens (~$0.017)

---

#### **DeepSeek**

1. Регистрация: https://platform.deepseek.com/sign_up
2. API Keys: https://platform.deepseek.com/api_keys
3. Create New Key
4. Скопировать ключ (формат: `sk-...`)

**Цены:**
- DeepSeek-Chat: $0.14/1M input tokens, $0.28/1M output tokens
- DeepSeek-Coder: $0.14/1M input tokens, $0.28/1M output tokens
- DeepSeek-V3: $0.14/1M input tokens, $0.28/1M output tokens

**💡 Примечание:** 1M токенов ≈ 750K слов английского текста

---

### **Шаг 3: Добавить API ключи через Swagger UI**

1. Откройте Swagger UI:
   ```
   https://your-ai-service.com/swagger-ui/index.html
   ```

2. Авторизуйтесь как admin:
   ```
   POST /api/auth/login
   {
     "username": "admin",
     "password": "your_admin_password"
   }
   ```

3. Добавьте API ключ для Qwen:
   ```
   POST /api/admin/networks/{networkId}/api-key
   {
     "apiKey": "sk-your-qwen-api-key"
   }
   ```

4. Добавьте API ключ для DeepSeek:
   ```
   POST /api/admin/networks/{networkId}/api-key
   {
     "apiKey": "sk-your-deepseek-api-key"
   }
   ```

5. Активируйте модели:
   ```
   PUT /api/admin/networks/{networkId}
   {
     "isActive": true
   }
   ```

---

### **Шаг 4: Дать доступ клиентскому приложению**

1. Найдите ID приложения (например, `noteapp`):
   ```
   GET /api/admin/clients
   ```

2. Найдите ID нейросети (например, `qwen-turbo`):
   ```
   GET /api/admin/networks
   ```

3. Создайте доступ:
   ```
   POST /api/admin/clients/{clientId}/networks/{networkId}
   {
     "dailyRequestLimit": 100,
     "monthlyRequestLimit": 1000,
     "priority": 10
   }
   ```

---

## 🧪 Тестирование

### **Тест 1: Проверка доступных моделей**

```bash
curl -X GET "https://your-ai-service.com/api/ai/networks/available" \
  -H "X-API-Key: your-noteapp-api-key"
```

**Ожидаемый результат:**
```json
{
  "networks": [
    {
      "id": "...",
      "name": "qwen-turbo",
      "displayName": "Qwen-Turbo (Fast & Cheap)",
      "provider": "qwen",
      "networkType": "chat",
      "isActive": true,
      "priority": 30
    },
    {
      "id": "...",
      "name": "deepseek-chat",
      "displayName": "DeepSeek-Chat (General)",
      "provider": "deepseek",
      "networkType": "chat",
      "isActive": true,
      "priority": 28
    }
  ]
}
```

---

### **Тест 2: Отправка запроса к Qwen**

```bash
curl -X POST "https://your-ai-service.com/api/ai/process" \
  -H "X-API-Key: your-noteapp-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "networkType": "chat",
    "messages": [
      {"role": "user", "content": "Привет! Как дела?"}
    ],
    "networkName": "qwen-turbo"
  }'
```

**Ожидаемый результат:**
```json
{
  "requestId": "...",
  "status": "success",
  "networkUsed": "qwen-turbo",
  "response": {
    "choices": [
      {
        "message": {
          "role": "assistant",
          "content": "Привет! У меня всё хорошо, спасибо! Как я могу помочь вам сегодня?"
        }
      }
    ]
  }
}
```

---

### **Тест 3: Отправка запроса к DeepSeek-V3**

```bash
curl -X POST "https://your-ai-service.com/api/ai/process" \
  -H "X-API-Key: your-noteapp-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "networkType": "chat",
    "messages": [
      {"role": "system", "content": "Ты полезный AI ассистент"},
      {"role": "user", "content": "Напиши функцию на Python для подсчета факториала"}
    ],
    "networkName": "deepseek-v3"
  }'
```

**Ожидаемый результат:**
```json
{
  "requestId": "...",
  "status": "success",
  "networkUsed": "deepseek-v3",
  "response": {
    "choices": [
      {
        "message": {
          "role": "assistant",
          "content": "```python\ndef factorial(n):\n    if n == 0 or n == 1:\n        return 1\n    return n * factorial(n - 1)\n```"
        }
      }
    ]
  }
}
```

---

## 📊 Сравнение цен

| Модель | Входные токены | Выходные токены | Примерная стоимость 1M токенов |
|--------|----------------|-----------------|--------------------------------|
| GPT-4o | $5.00/1M | $15.00/1M | ~$10/1M (среднее) |
| GPT-4o-mini | $0.15/1M | $0.60/1M | ~$0.38/1M (среднее) |
| **DeepSeek-Chat** | **$0.14/1M** | **$0.28/1M** | **~$0.21/1M** ⭐ |
| **DeepSeek-V3** | **$0.14/1M** | **$0.28/1M** | **~$0.21/1M** ⭐ |
| **Qwen-Turbo** | **$1.10/1M** | **$1.10/1M** | **~$1.10/1M** |
| Qwen-Plus | $5.50/1M | $5.50/1M | ~$5.50/1M |
| Claude 3.5 Sonnet | $3.00/1M | $15.00/1M | ~$9/1M (среднее) |

**💰 Выводы:**
- DeepSeek: **в 47 раз дешевле GPT-4o**
- DeepSeek: **в 1.8 раз дешевле GPT-4o-mini**
- Qwen-Turbo: **в 9 раз дешевле GPT-4o**
- DeepSeek-V3: **конкурирует с GPT-4o по качеству, но намного дешевле**

---

## 🚀 Рекомендации по использованию

### **Для noteapp:**

1. **Основная модель для заметок: DeepSeek-Chat**
   - Низкая стоимость
   - Высокое качество
   - Хорошая поддержка русского

2. **Для кода: DeepSeek-Coder**
   - Специализирована для программирования
   - Отличное качество генерации кода

3. **Для быстрых ответов: Qwen-Turbo**
   - Самая быстрая
   - Дешевая
   - Хорошо для простых задач

4. **Для сложных задач: DeepSeek-V3 или Qwen2.5-72B**
   - Максимальное качество
   - Все еще дешевле GPT-4
   - Для важных запросов

---

## 📋 Структура priority

| Priority | Модель | Когда использовать |
|----------|--------|-------------------|
| 10 | GPT-4o, Whisper | Премиум функции |
| 15 | GPT-4o-mini, Claude 3 Opus | Стандартные запросы |
| 16 | **DeepSeek-V3** | **Сложные задачи (дешево!)** |
| 18 | **Qwen2.5-72B** | **Альтернатива DeepSeek-V3** |
| 20 | Qwen-Max | Сложные задачи (дороже) |
| 25 | Qwen-Plus | Средние задачи |
| 26 | **DeepSeek-Coder** | **Код (дешево!)** |
| 28 | **DeepSeek-Chat** | **По умолчанию (дешево!)** |
| 30 | **Qwen-Turbo** | **Быстрые ответы** |

**💡 Рекомендация:** Установите DeepSeek-Chat как модель по умолчанию для экономии средств!

---

**🎉 Qwen и DeepSeek успешно интегрированы в AI Integration Service! 🎉**

