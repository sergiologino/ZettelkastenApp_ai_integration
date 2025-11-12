-- ===================================================
-- Проверка и добавление API ключа для Whisper
-- ===================================================

-- 1. Проверить, есть ли API ключ у нейросети Whisper
SELECT 
    id,
    name,
    display_name,
    provider,
    api_url,
    CASE 
        WHEN api_key_encrypted IS NULL OR api_key_encrypted = '' THEN '❌ НЕТ'
        ELSE '✅ ЕСТЬ'
    END as "API Key Status",
    is_active
FROM neural_networks
WHERE name = 'whisper' OR provider = 'whisper';

-- ===================================================
-- Если API ключ отсутствует, добавьте его:
-- ===================================================

-- ВНИМАНИЕ: Замените 'YOUR_OPENAI_API_KEY' на ваш реальный OpenAI API ключ!
-- Получить ключ можно здесь: https://platform.openai.com/account/api-keys

-- Вариант 1: Обновить существующую нейросеть (если она уже есть)
-- UPDATE neural_networks
-- SET api_key_encrypted = 'YOUR_OPENAI_API_KEY'  -- ⚠️ ЗАМЕНИТЕ НА ВАШИХ КЛЮЧ!
-- WHERE name = 'whisper';

-- Вариант 2: Проверить результат
-- SELECT 
--     name,
--     CASE 
--         WHEN api_key_encrypted IS NULL OR api_key_encrypted = '' THEN '❌ НЕТ'
--         ELSE '✅ ЕСТЬ (длина: ' || LENGTH(api_key_encrypted) || ')'
--     END as "API Key Status"
-- FROM neural_networks
-- WHERE name = 'whisper';

-- ===================================================
-- ВАЖНО: Использование через UI (рекомендуется)
-- ===================================================

/*
Вместо прямого SQL запроса, рекомендуется добавить API ключ через UI:

1. Откройте AI Integration Service Swagger UI:
   https://your-ai-service.com/swagger-ui.html

2. Авторизуйтесь (если требуется)

3. Найдите раздел "Admin Neural Networks"

4. Используйте метод PUT /api/admin/networks/{id}

5. Обновите нейросеть Whisper, добавив поле:
   {
     "apiKey": "sk-ваш-openai-api-ключ"
   }

Swagger UI автоматически зашифрует ключ перед сохранением в БД.
*/

-- ===================================================
-- Проверка шифрования ключа
-- ===================================================

-- Проверить, зашифрован ли ключ (должен начинаться с ENC()
-- SELECT 
--     name,
--     CASE 
--         WHEN api_key_encrypted LIKE 'ENC(%' THEN '✅ Зашифрован'
--         WHEN api_key_encrypted LIKE 'sk-%' THEN '⚠️ НЕ зашифрован (plaintext)'
--         ELSE '❓ Неизвестный формат'
--     END as "Encryption Status",
--     LEFT(api_key_encrypted, 20) || '...' as "Key Preview"
-- FROM neural_networks
-- WHERE name = 'whisper';

