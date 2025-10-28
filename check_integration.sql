-- Проверка интеграции noteapp с ai-integration
-- Выполните этот скрипт в базе данных ai_integration_db

-- 1. Проверяем, есть ли клиент noteapp
SELECT 'Клиенты в ai-integration:' as info;
SELECT id, name, description, api_key_encrypted IS NOT NULL as has_api_key, is_active, created_at 
FROM client_applications 
WHERE name = 'noteapp';

-- 2. Проверяем доступы клиента noteapp к нейросетям
SELECT 'Доступы noteapp к нейросетям:' as info;
SELECT 
    cna.id,
    ca.name as client_name,
    nn.name as network_name,
    nn.display_name as network_display_name,
    nn.network_type,
    nn.is_active as network_active,
    cna.daily_request_limit,
    cna.monthly_request_limit,
    cna.created_at
FROM client_network_access cna
JOIN client_applications ca ON cna.client_application_id = ca.id
JOIN neural_networks nn ON cna.neural_network_id = nn.id
WHERE ca.name = 'noteapp'
ORDER BY nn.network_type, nn.name;

-- 3. Проверяем активные нейросети
SELECT 'Активные нейросети:' as info;
SELECT id, name, display_name, provider, network_type, is_active 
FROM neural_networks 
WHERE is_active = true
ORDER BY network_type, name;

-- 4. Общая статистика доступов
SELECT 'Общая статистика доступов:' as info;
SELECT 
    COUNT(*) as total_accesses,
    COUNT(DISTINCT client_application_id) as clients_with_access,
    COUNT(DISTINCT neural_network_id) as networks_with_access,
    COUNT(CASE WHEN daily_request_limit IS NOT NULL OR monthly_request_limit IS NOT NULL THEN 1 END) as accesses_with_limits,
    COUNT(CASE WHEN daily_request_limit IS NULL AND monthly_request_limit IS NULL THEN 1 END) as unlimited_accesses
FROM client_network_access;
