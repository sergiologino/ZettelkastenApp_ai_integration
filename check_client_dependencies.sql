-- Проверка связанных записей для клиента "test"
-- Выполните этот SQL в базе данных ai_integration_db

-- 1. Найдем клиента "test"
SELECT id, name, description, is_active, created_at 
FROM client_applications 
WHERE name = 'test';

-- 2. Проверим, есть ли связанные записи в client_network_access
SELECT 
    cna.id,
    ca.name as client_name,
    nn.name as network_name,
    cna.created_at
FROM client_network_access cna
JOIN client_applications ca ON cna.client_application_id = ca.id
JOIN neural_networks nn ON cna.neural_network_id = nn.id
WHERE ca.name = 'test';

-- 3. Проверим, есть ли связанные записи в request_logs
SELECT 
    rl.id,
    rl.status,
    rl.created_at,
    ca.name as client_name
FROM request_logs rl
JOIN client_applications ca ON rl.client_application_id = ca.id
WHERE ca.name = 'test'
ORDER BY rl.created_at DESC
LIMIT 10;

-- 4. Проверим, есть ли связанные записи в external_users
SELECT 
    eu.id,
    eu.external_user_id,
    eu.created_at,
    ca.name as client_name
FROM external_users eu
JOIN client_applications ca ON eu.client_application_id = ca.id
WHERE ca.name = 'test';
