-- SQL скрипт для исправления ошибки Flyway
-- Выполните эти команды в базе данных PostgreSQL

-- Вариант 1: Удалить запись о миграции V004 (если она не была применена корректно)
DELETE FROM flyway_schema_history WHERE version = '004';

-- Вариант 2: Обновить checksum миграции V004 (если нужно сохранить запись)
-- UPDATE flyway_schema_history 
-- SET checksum = -1622964891 
-- WHERE version = '004';

-- Вариант 3: Отметить миграцию как удаленную
-- UPDATE flyway_schema_history 
-- SET success = false, 
--     description = 'DELETED - Migration file removed intentionally'
-- WHERE version = '004';

-- Проверить текущее состояние миграций
SELECT version, description, success, checksum, installed_on 
FROM flyway_schema_history 
ORDER BY installed_rank;

-- После выполнения одного из вариантов выше, запустите приложение
-- Новая миграция V005 будет применена автоматически
