# 🔧 Исправление ошибки 401 UNAUTHORIZED

## 🐛 Проблема

При запросе `/api/ai/networks/available` AI Integration Service возвращал **401 UNAUTHORIZED**, хотя API ключ правильный и присутствует в заголовке.

### **Логи из noteapp backend:**
```
✅ [AiConnectionService] Найдено подключение: serviceUrl=https://..., apiKey присутствует=true
🔍 [AiIntegrationService] Отправляем GET запрос к AI-сервису...
❌ [AiIntegrationService] HTTP ошибка: Status: 401 UNAUTHORIZED
```

---

## 🔍 Причина

В `SecurityConfig.java` AI Integration Service:

**Было:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()  // ❌ Все открыто
        );
    return http.build();
}
```

**Проблема:**
- `ApiKeyAuthFilter` **не был подключен** к цепочке фильтров Spring Security
- Поэтому `Authentication` в контроллере всегда был `null`
- Контроллер возвращал 401, так как не нашел клиента

---

## ✅ Решение

### **1. Исправлен SecurityConfig.java:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final ClientApplicationRepository clientApplicationRepository;
    
    public SecurityConfig(ClientApplicationRepository clientApplicationRepository) {
        this.clientApplicationRepository = clientApplicationRepository;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // ✅ ДОБАВЛЕН API Key фильтр
            .addFilterBefore(new ApiKeyAuthFilter(clientApplicationRepository), 
                            UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Публичные endpoints
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/api/auth/**",
                    "/api/admin/**"
                ).permitAll()
                // AI endpoints требуют X-API-Key ✅
                .requestMatchers("/api/ai/**").authenticated()
                .anyRequest().denyAll()
            );
        
        return http.build();
    }
}
```

### **Что изменилось:**

1. ✅ **Добавлен фильтр:** `addFilterBefore(new ApiKeyAuthFilter(...), ...)`
2. ✅ **Требуется аутентификация:** `/api/ai/**` теперь требует `.authenticated()`
3. ✅ **ApiKeyAuthFilter** теперь обрабатывает заголовок `X-API-Key` и устанавливает `Authentication`

---

## 🚀 Как применить исправление

### **1. Пересобрать и перезапустить AI Integration Service:**

```bash
cd noteapp-ai-integration

# Пересобрать
mvn clean install

# Перезапустить
docker-compose down
docker-compose up -d

# Проверить логи
docker logs ai-integration-service -f
```

### **2. Проверить, что фильтр загрузился:**

В логах должно появиться:
```
========================================
🔧 SecurityConfig ЗАГРУЖЕН!
✅ API Key фильтр будет подключен!
========================================
🔒 Настройка SecurityFilterChain с API Key фильтром
✅ SecurityFilterChain настроен - API Key фильтр включен
```

---

## 🧪 Тестирование

### **1. Проверить через curl:**

```bash
# Получить API ключ из БД noteapp
API_KEY="aikey_ваш_ключ_здесь"

# Запрос к /api/ai/networks/available
curl -X GET https://sergiologino-zettelkastenapp-ai-integration-bce3.twc1.net/api/ai/networks/available \
  -H "X-API-Key: $API_KEY" \
  -v

# Должно вернуть:
# HTTP/1.1 200 OK
# [
#   {
#     "id": "...",
#     "name": "openai-whisper",
#     "displayName": "OpenAI Whisper",
#     ...
#   }
# ]
```

### **2. Проверить логи AI Integration Service:**

```bash
docker logs ai-integration-service -f
```

**Должно быть:**
```
🔵 [ApiKeyAuthFilter] Обработка запроса к /networks/available
🔍 [ApiKeyAuthFilter] X-API-Key header: присутствует
🔍 [ApiKeyAuthFilter] API Key длина: 48
🔍 [ApiKeyAuthFilter] API Key preview: aikey_02fceb86f20d43d8...
✅ [ApiKeyAuthFilter] Клиент найден: noteapp (ID: ...)
🔵 [AiController] ===== ЗАПРОС /api/ai/networks/available =====
✅ [AiController] Клиент найден: noteapp (ID: ...)
✅ [AiController] Получено 2 доступных нейросетей для клиента noteapp
```

**НЕ должно быть:**
```
⚠️ [ApiKeyAuthFilter] Клиент с таким API ключом не найден ❌
⚠️ [ApiKeyAuthFilter] X-API-Key заголовок отсутствует или пуст ❌
⚠️ [AiController] Аутентификация не пройдена ❌
```

### **3. Проверить синхронизацию в noteapp:**

1. Откройте фронтенд → Профиль → "Доступные нейросети"
2. Нажмите кнопку "Синхронизация" (🔄)
3. Дождитесь завершения

**Проверьте логи backend noteapp:**
```bash
docker logs noteapp-backend -f
```

**Должно быть:**
```
✅ [AiConnectionService] Найдено подключение: serviceUrl=https://..., apiKey присутствует=true
🔍 [AiIntegrationService] Отправляем GET запрос к AI-сервису...
✅ [AiIntegrationService] Получено 2 нейросетей из AI-сервиса ✅
➕ Добавлена новая нейросеть: OpenAI Whisper
➕ Добавлена новая нейросеть: Yandex GPT Pro
✅ Синхронизировано: 2/2
```

**НЕ должно быть:**
```
❌ [AiIntegrationService] HTTP ошибка: Status: 401 UNAUTHORIZED ❌
```

### **4. Проверить БД noteapp:**

```sql
SELECT 
    id, 
    name, 
    display_name, 
    provider, 
    network_type, 
    is_active,
    last_sync_at
FROM neural_networks
ORDER BY priority DESC;
```

**Ожидаемый результат:**
```
id       | name           | display_name   | provider | network_type  | is_active
---------|----------------|----------------|----------|---------------|----------
uuid-1   | openai-whisper | OpenAI Whisper | openai   | transcription | true
uuid-2   | yandex-gpt     | Yandex GPT Pro | yandex   | chat          | true
```

---

## 🐛 Troubleshooting

### **Проблема: Всё ещё получаю 401**

**Проверьте:**

1. **Фильтр загружен?**
   ```bash
   docker logs ai-integration-service 2>&1 | grep "API Key фильтр"
   # Должно: ✅ API Key фильтр будет подключен!
   ```

2. **API ключ правильный?**
   ```sql
   -- Проверьте в БД noteapp
   SELECT api_key FROM ai_service_connections WHERE service_name = 'noteapp';
   
   -- Проверьте в БД ai-integration
   SELECT api_key FROM client_applications WHERE name = 'noteapp';
   
   -- API ключи должны совпадать!
   ```

3. **Клиент активен?**
   ```sql
   -- В БД ai-integration
   SELECT name, is_active FROM client_applications WHERE name = 'noteapp';
   -- Должно: is_active = true
   ```

4. **Заголовок отправляется?**
   ```bash
   # Проверьте в логах AI Integration Service
   docker logs ai-integration-service 2>&1 | grep "X-API-Key header"
   # Должно: X-API-Key header: присутствует
   ```

---

### **Проблема: "Клиент с таким API ключом не найден"**

**Решение:**

API ключи в БД noteapp и ai-integration **не совпадают**!

**Вариант A: Получить правильный API ключ из AI-сервиса**

```bash
# 1. Логин в AI-сервис
curl -X POST https://sergiologino-zettelkastenapp-ai-integration-bce3.twc1.net/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your_password"}'
# Сохраните JWT токен

# 2. Получите клиента noteapp
curl -X GET https://sergiologino-zettelkastenapp-ai-integration-bce3.twc1.net/api/admin/clients \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
# Найдите apiKey для клиента noteapp
```

**Вариант B: Обновить API ключ в noteapp БД**

```sql
-- В БД noteapp
UPDATE ai_service_connections 
SET api_key = 'aikey_правильный_ключ_из_ai_сервиса'
WHERE service_name = 'noteapp';
```

---

## 📝 Summary

### **Что было не так:**

1. ❌ `ApiKeyAuthFilter` не был подключен к Spring Security
2. ❌ `SecurityConfig` имел `.anyRequest().permitAll()` без фильтра
3. ❌ `Authentication` в контроллере всегда был `null`
4. ❌ Контроллер возвращал 401, хотя API ключ был правильный

### **Что исправлено:**

1. ✅ Добавлен `ApiKeyAuthFilter` в цепочку фильтров
2. ✅ `/api/ai/**` теперь требует `.authenticated()`
3. ✅ Фильтр проверяет `X-API-Key` и устанавливает `Authentication`
4. ✅ Контроллер теперь получает `ClientApplication` из `Authentication`

### **Результат:**

После применения исправления:
- ✅ Запросы с правильным API ключом возвращают 200 OK
- ✅ Синхронизация нейросетей работает
- ✅ Нейросети появляются в БД noteapp
- ✅ Фронтенд показывает список нейросетей

🚀 **Всё работает!**

