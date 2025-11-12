# 🐛 Исправление: Отсутствует API ключ OpenAI для Whisper

## 📊 Проблема

### **Симптомы:**
- Запрос к Whisper API возвращает **401 Unauthorized**
- Ошибка: `"You didn't provide an API key"`

### **Ошибка в логе:**

```
org.springframework.web.client.HttpClientErrorException$Unauthorized: 401 Unauthorized on POST request for "https://api.openai.com/v1/audio/transcriptions": 
{
    "error": {
        "message": "You didn't provide an API key. You need to provide your API key in an Authorization header using Bearer auth (i.e. Authorization: Bearer YOUR_KEY)",
        "type": "invalid_request_error"
    }
}
```

---

## 🔍 Причина

**Файл:** `noteapp-ai-integration/src/main/java/com/example/integration/client/WhisperClient.java`

**Код (строки 41-45):**
```java
if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
    String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
    headers.set("Authorization", "Bearer " + decryptedKey);
}
```

**Проблема:**
- `network.getApiKeyEncrypted()` возвращает `null` или пустую строку
- Authorization header **не устанавливается**
- OpenAI Whisper API получает запрос **без API ключа**
- Результат: **401 Unauthorized**

**Причина:** В таблице `neural_networks` для нейросети `whisper` отсутствует `api_key_encrypted`.

---

## ✅ Решение

### **Решение №1: Улучшено логирование и обработка ошибок**

**Файл:** `noteapp-ai-integration/src/main/java/com/example/integration/client/WhisperClient.java`

**Стало (строки 42-61):**
```java
System.out.println("🔑 [WhisperClient] Проверяем API ключ для Whisper:");
System.out.println("🔑 [WhisperClient]   - Network ID: " + network.getId());
System.out.println("🔑 [WhisperClient]   - Network name: " + network.getName());
System.out.println("🔑 [WhisperClient]   - API key encrypted присутствует: " + 
    (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()));

if (network.getApiKeyEncrypted() != null && !network.getApiKeyEncrypted().isEmpty()) {
    // ✅ Расшифровываем ключ для Whisper API
    try {
        String decryptedKey = encryptionService.decrypt(network.getApiKeyEncrypted());
        headers.set("Authorization", "Bearer " + decryptedKey);
        System.out.println("✅ [WhisperClient] Authorization header установлен (Bearer ***" + 
            decryptedKey.substring(Math.max(0, decryptedKey.length() - 4)) + ")");
    } catch (Exception e) {
        System.err.println("❌ [WhisperClient] Ошибка расшифровки API ключа: " + e.getMessage());
        throw new RuntimeException("Ошибка расшифровки API ключа для Whisper: " + e.getMessage(), e);
    }
} else {
    System.err.println("❌ [WhisperClient] API ключ для Whisper отсутствует!");
    System.err.println("❌ [WhisperClient] Необходимо добавить API ключ OpenAI для нейросети 'whisper' в админ-панели AI Integration Service");
    throw new RuntimeException("API ключ для Whisper отсутствует. Добавьте API ключ OpenAI в настройках нейросети.");
}
```

**Изменения:**
- ✅ Добавлено логирование проверки API ключа
- ✅ Добавлена обработка ошибок расшифровки
- ✅ Если ключ отсутствует - выбрасывается исключение с понятным сообщением
- ✅ Логируется последние 4 символа ключа для проверки

---

### **Решение №2: Добавить API ключ OpenAI**

## 🔧 **Способ 1: Через UI AI Integration Service (Рекомендуется)**

### **Шаг 1: Получите OpenAI API ключ**

1. Откройте: https://platform.openai.com/account/api-keys
2. Войдите в свой аккаунт OpenAI
3. Нажмите "Create new secret key"
4. Скопируйте ключ (начинается с `sk-...`)

### **Шаг 2: Откройте Swagger UI AI Integration Service**

```
https://your-ai-service.com/swagger-ui.html
```

### **Шаг 3: Найдите ID нейросети Whisper**

Используйте метод: **GET /api/admin/networks**

Найдите в ответе:
```json
{
  "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "name": "whisper",
  "displayName": "Whisper (Audio Transcription)",
  "provider": "whisper"
}
```

Скопируйте `id`.

### **Шаг 4: Обновите нейросеть с API ключом**

Используйте метод: **PUT /api/admin/networks/{id}**

В теле запроса:
```json
{
  "apiKey": "sk-ваш-openai-api-ключ-здесь"
}
```

Swagger UI автоматически:
- Зашифрует ключ
- Сохранит в `api_key_encrypted` как `ENC(...)`

---

## 🔧 **Способ 2: Через SQL (Не рекомендуется)**

**⚠️ ВАЖНО:** Этот способ **не шифрует** ключ! Используйте только для тестирования.

### **Шаг 1: Проверьте текущее состояние**

```sql
SELECT 
    id,
    name,
    display_name,
    CASE 
        WHEN api_key_encrypted IS NULL OR api_key_encrypted = '' THEN '❌ НЕТ'
        ELSE '✅ ЕСТЬ'
    END as "API Key Status"
FROM neural_networks
WHERE name = 'whisper';
```

### **Шаг 2: Добавьте API ключ (plaintext)**

```sql
UPDATE neural_networks
SET api_key_encrypted = 'sk-ваш-openai-api-ключ'  -- ⚠️ НЕ ШИФРУЕТСЯ!
WHERE name = 'whisper';
```

**⚠️ Проблема:** Ключ хранится в открытом виде (plaintext), что **небезопасно**.

**✅ Рекомендация:** Используйте **Способ 1 (через UI)** для автоматического шифрования.

---

## 🧪 Тестирование

### **1. Проверьте логи AI Integration Service:**

```bash
docker logs ai-integration-service -f | grep WhisperClient
```

**Ожидаемые логи (ПОСЛЕ добавления API ключа):**

```
🔑 [WhisperClient] Проверяем API ключ для Whisper:
🔑 [WhisperClient]   - Network ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
🔑 [WhisperClient]   - Network name: whisper
🔑 [WhisperClient]   - API key encrypted присутствует: true  ✅
✅ [WhisperClient] Authorization header установлен (Bearer ***AB12)  ✅
✅ [WhisperClient] URL после добавления пути: https://api.openai.com/v1/audio/transcriptions
🎤 [WhisperClient] Отправляем запрос к Whisper API: https://api.openai.com/v1/audio/transcriptions
✅ [WhisperClient] Получен ответ от Whisper API, status: 200 OK  ✅
```

**Если ключ отсутствует:**

```
🔑 [WhisperClient] API key encrypted присутствует: false  ❌
❌ [WhisperClient] API ключ для Whisper отсутствует!
❌ [WhisperClient] Необходимо добавить API ключ OpenAI для нейросети 'whisper' в админ-панели
```

### **2. Отправьте голосовое сообщение в Telegram бот**

### **3. Проверьте заметку в UI:**

- Откройте фронтенд
- Заметка должна появиться СРАЗУ ✅
- С транскрибацией ✅

```markdown
---
🎤 **Транскрибация** (Whisper (Audio Transcription)):

[Текст вашего голосового сообщения]

---
```

---

## 🔐 Проверка шифрования API ключа

### **SQL запрос:**

```sql
SELECT 
    name,
    CASE 
        WHEN api_key_encrypted LIKE 'ENC(%' THEN '✅ Зашифрован (через UI)'
        WHEN api_key_encrypted LIKE 'sk-%' THEN '⚠️ Plaintext (через SQL)'
        ELSE '❓ Неизвестный формат'
    END as "Encryption Status",
    LEFT(api_key_encrypted, 20) || '...' as "Key Preview"
FROM neural_networks
WHERE name = 'whisper';
```

**Ожидаемый результат (через UI):**
```
name    | Encryption Status             | Key Preview
--------|-------------------------------|------------------
whisper | ✅ Зашифрован (через UI)      | ENC(AES256:base64...
```

**Если через SQL (не рекомендуется):**
```
name    | Encryption Status             | Key Preview
--------|-------------------------------|------------------
whisper | ⚠️ Plaintext (через SQL)      | sk-proj-abc123456...
```

---

## 📋 Summary изменений

### **Изменённый файл:**

**`noteapp-ai-integration/src/main/java/com/example/integration/client/WhisperClient.java`**

**Строки 42-61:**
- ✅ Добавлено логирование проверки API ключа
- ✅ Добавлена обработка ошибок расшифровки
- ✅ Если ключ отсутствует - выбрасывается исключение с понятным сообщением

### **Созданные файлы:**

**`noteapp-ai-integration/CHECK_WHISPER_API_KEY.sql`**
- SQL скрипт для проверки и добавления API ключа

---

## ⚠️ Важно

### **Где хранятся API ключи?**

- **База данных:** `ai-integration` (PostgreSQL)
- **Таблица:** `neural_networks`
- **Поле:** `api_key_encrypted`

### **Формат ключа:**

1. **Через UI (рекомендуется):**
   ```
   ENC(AES256:base64-encoded-encrypted-data)
   ```
   Расшифровывается `EncryptionService.decrypt()`

2. **Через SQL (не рекомендуется):**
   ```
   sk-proj-abcdefghijklmnopqrstuvwxyz123456
   ```
   Хранится в открытом виде (небезопасно!)

### **Безопасность:**

- ✅ **Используйте UI** для автоматического шифрования
- ❌ **Не храните ключи в plaintext**
- ✅ Регулярно ротируйте API ключи
- ✅ Используйте разные ключи для разных окружений (dev/prod)

---

## 🔗 Полезные ссылки

- **OpenAI API Keys:** https://platform.openai.com/account/api-keys
- **OpenAI Whisper API Docs:** https://platform.openai.com/docs/api-reference/audio
- **AI Integration Service Swagger:** `https://your-ai-service.com/swagger-ui.html`

---

**После добавления API ключа OpenAI транскрибация заработает! 🎉**

