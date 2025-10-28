-- SQL скрипт для добавления записи о миграции V005 в flyway_schema_history
-- Выполните эту команду в базе данных PostgreSQL

-- Добавляем запись о том, что миграция V005 уже применена
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
) VALUES (
    4,  -- следующий ранг после V003
    '005',
    'add client network access',
    'SQL',
    'V005__add_client_network_access.sql',
    -1622964891,  -- checksum для этой миграции
    'postgres',   -- или ваш пользователь БД
    NOW(),
    0,
    true
);

-- Проверим результат
SELECT version, description, success, checksum, installed_on 
FROM flyway_schema_history 
ORDER BY installed_rank;
