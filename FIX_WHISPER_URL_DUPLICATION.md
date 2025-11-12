# 🐛 Исправление: Дублирование /v1/ в URL Whisper API

## 📊 Проблема

### **Симптомы:**
- Запрос к Whisper API возвращает **404 Not Found**
- URL содержит дублирование `/v1/v1/`

### **Ошибка в логе:**

```json
{
  "status": "failed",
  "errorMessage": "404 Not Found on POST request for \"https://api.openai.com/v1/v1/audio/transcriptions\": [no body]"
}
```

**Неправильный URL:** `https://api.openai.com/v1/v1/audio/transcriptions` ❌  
**Правильный URL:** `https://api.openai.com/v1/audio/transcriptions` ✅

---

## 🔍 Причина

### **Файл:** `noteapp-ai-integration/src/main/java/com/example/integration/client/WhisperClient.java`

**Было (строки 67-70):**
```java
// Отправляем запрос
String url = network.getApiUrl();  // "https://api.openai.com/v1"
if (!url.contains("/audio/transcriptions")) {
    url = url + "/v1/audio/transcriptions";  // ❌ Добавляет /v1/ еще раз
}
```

**Проблема:**
1. `network.getApiUrl()` в БД: `https://api.openai.com/v1`
2. Код проверяет только наличие `/audio/transcriptions`
3. Код всегда добавляет `/v1/audio/transcriptions`
4. **Результат:** `https://api.openai.com/v1` + `/v1/audio/transcriptions` = `/v1/v1/audio/transcriptions` ❌

---

## ✅ Решение

### **Улучшена логика формирования URL**

**Файл:** `noteapp-ai-integration/src/main/java/com/example/integration/client/WhisperClient.java`

**Стало (строки 67-94):**
```java
// Отправляем запрос
String url = network.getApiUrl();

// ✅ ИСПРАВЛЕНО: Проверяем, содержит ли URL уже полный путь
if (url.contains("/audio/transcriptions")) {
    // URL уже содержит полный путь, используем как есть
    System.out.println("🔍 [WhisperClient] URL уже содержит /audio/transcriptions: " + url);
} else if (url.endsWith("/v1") || url.endsWith("/v1/")) {
    // URL заканчивается на /v1, добавляем только /audio/transcriptions
    url = url.replaceAll("/+$", "") + "/audio/transcriptions";
    System.out.println("✅ [WhisperClient] URL после добавления пути: " + url);
} else {
    // URL не содержит /v1, добавляем полный путь
    url = url.replaceAll("/+$", "") + "/v1/audio/transcriptions";
    System.out.println("✅ [WhisperClient] URL после добавления /v1/audio/transcriptions: " + url);
}

System.out.println("🎤 [WhisperClient] Отправляем запрос к Whisper API: " + url);
System.out.println("🎤 [WhisperClient] Model: " + (network.getModelName() != null ? network.getModelName() : "whisper-1"));

ResponseEntity<Map> response = restTemplate.exchange(
    url,
    HttpMethod.POST,
    request,
    Map.class
);

System.out.println("✅ [WhisperClient] Получен ответ от Whisper API, status: " + response.getStatusCode());
```

**Изменения:**
- ✅ Проверка: уже содержит `/audio/transcriptions`? → использовать как есть
- ✅ Проверка: заканчивается на `/v1`? → добавить только `/audio/transcriptions`
- ✅ Иначе: добавить полный путь `/v1/audio/transcriptions`
- ✅ Удаление trailing slashes с помощью `replaceAll("/+$", "")`
- ✅ Подробное логирование формирования URL

---

## 🧪 Примеры работы

### **Пример 1: URL с /v1**
```
Вход:  network.getApiUrl() = "https://api.openai.com/v1"
Выход: url = "https://api.openai.com/v1/audio/transcriptions" ✅
```

### **Пример 2: URL с /v1/**
```
Вход:  network.getApiUrl() = "https://api.openai.com/v1/"
Выход: url = "https://api.openai.com/v1/audio/transcriptions" ✅
```

### **Пример 3: URL без /v1**
```
Вход:  network.getApiUrl() = "https://api.openai.com"
Выход: url = "https://api.openai.com/v1/audio/transcriptions" ✅
```

### **Пример 4: URL с полным путем**
```
Вход:  network.getApiUrl() = "https://api.openai.com/v1/audio/transcriptions"
Выход: url = "https://api.openai.com/v1/audio/transcriptions" ✅
```

---

## 🧪 Тестирование

### **1. Проверьте логи AI Integration Service:**

```bash
docker logs ai-integration-service -f | grep WhisperClient
```

**Ожидаемые логи:**

```
✅ [WhisperClient] URL после добавления пути: https://api.openai.com/v1/audio/transcriptions
🎤 [WhisperClient] Отправляем запрос к Whisper API: https://api.openai.com/v1/audio/transcriptions
🎤 [WhisperClient] Model: whisper-1
✅ [WhisperClient] Получен ответ от Whisper API, status: 200 OK
```

### **2. Отправьте голосовое сообщение в Telegram бот**

### **3. Проверьте заметку в UI:**

- Откройте фронтенд
- Найдите созданную заметку
- В содержимом должна быть транскрибация:

```markdown
---
🎤 **Транскрибация** (Whisper (Audio Transcription)):

[Текст вашего голосового сообщения]

---
```

---

## 📋 Summary изменений

### **Изменённый файл:**

**`noteapp-ai-integration/src/main/java/com/example/integration/client/WhisperClient.java`**

**Метод:** `sendRequest()` (строки 67-94)

**Изменения:**
- Улучшена логика формирования URL
- Добавлено 3 варианта обработки URL
- Добавлено логирование формирования URL
- Добавлено логирование ответа от Whisper API

---

## 🔧 Как это работает

### **Логика формирования URL:**

```
1. Получаем apiUrl из БД: "https://api.openai.com/v1"
   ↓
2. Проверяем, содержит ли "/audio/transcriptions"? НЕТ
   ↓
3. Проверяем, заканчивается на "/v1"? ДА
   ↓
4. Убираем trailing slash: "https://api.openai.com/v1"
   ↓
5. Добавляем "/audio/transcriptions"
   ↓
6. Итоговый URL: "https://api.openai.com/v1/audio/transcriptions" ✅
```

---

## ⚠️ Важно

### **Значения api_url в БД:**

Проверьте таблицу `neural_networks` в БД `noteapp-ai-integration`:

```sql
SELECT name, api_url FROM neural_networks WHERE provider = 'whisper';
```

**Ожидаемый результат:**
```
name    | api_url
--------|-------------------------
whisper | https://api.openai.com/v1
```

Если `api_url` содержит полный путь (`/audio/transcriptions`), код будет использовать его как есть.

---

**Теперь Whisper API вызывается с правильным URL без дублирования! 🎉**

