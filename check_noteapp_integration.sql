-- ========================================
-- Проверка интеграции noteapp с AI Integration Service
-- ========================================

-- 1. Проверка регистрации клиента noteapp
SELECT 
    c.id AS client_id,
    c.name AS client_name,
    c.description,
    c.api_key,
    c.is_active,
    c.created_at,
    c.updated_at
FROM clients c
WHERE c.name = 'noteapp';

-- 2. Проверка доступных нейросетей
SELECT 
    n.id AS network_id,
    n.name AS network_name,
    n.display_name,
    n.provider,
    n.network_type,
    n.model_name,
    n.is_free,
    n.priority,
    n.is_active
FROM neural_networks n
WHERE n.is_active = true
ORDER BY n.priority DESC;

-- 3. Проверка связей noteapp с нейросетями (client_network_access)
SELECT 
    cna.id AS access_id,
    c.name AS client_name,
    n.name AS network_name,
    n.display_name,
    n.network_type,
    cna.has_access,
    cna.priority_override,
    cna.daily_limit,
    cna.monthly_limit,
    cna.remaining_requests_today,
    cna.remaining_requests_month,
    cna.last_reset_date
FROM client_network_access cna
JOIN clients c ON cna.client_id = c.id
JOIN neural_networks n ON cna.network_id = n.id
WHERE c.name = 'noteapp'
ORDER BY cna.priority_override DESC NULLS LAST, n.priority DESC;

-- 4. Проверка нейросетей БЕЗ связей с noteapp
SELECT 
    n.id AS network_id,
    n.name AS network_name,
    n.display_name,
    n.network_type,
    n.provider,
    n.priority,
    n.is_active
FROM neural_networks n
WHERE n.is_active = true
  AND NOT EXISTS (
    SELECT 1 
    FROM client_network_access cna
    JOIN clients c ON cna.client_id = c.id
    WHERE cna.network_id = n.id 
      AND c.name = 'noteapp'
  )
ORDER BY n.priority DESC;

-- 5. Количество нейросетей и связей
SELECT 
    (SELECT COUNT(*) FROM neural_networks WHERE is_active = true) AS total_active_networks,
    (SELECT COUNT(*) FROM clients WHERE name = 'noteapp') AS noteapp_clients_count,
    (SELECT COUNT(*) 
     FROM client_network_access cna
     JOIN clients c ON cna.client_id = c.id
     WHERE c.name = 'noteapp' AND cna.has_access = true) AS noteapp_network_access_count;

