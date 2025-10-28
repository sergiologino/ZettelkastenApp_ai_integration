-- SQL скрипт для проверки структуры существующей таблицы client_network_access
-- Выполните эту команду в базе данных PostgreSQL

-- Проверяем структуру таблицы
\d client_network_access;

-- Или альтернативный способ:
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_name = 'client_network_access' 
ORDER BY ordinal_position;

-- Проверяем существующие данные
SELECT COUNT(*) as total_records FROM client_network_access;

-- Проверяем индексы
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'client_network_access';
