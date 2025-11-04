# ========================================
# PowerShell скрипт для проверки интеграции noteapp
# ========================================

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "Проверка интеграции noteapp с AI Service" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Подключение к БД AI Integration Service
# ВАЖНО: Измените параметры подключения под ваши настройки
$DB_HOST = "localhost"
$DB_PORT = "5432"
$DB_NAME = "ai_integration_db"
$DB_USER = "postgres"
$DB_PASSWORD = "Vfrcbvjd1986"

$env:PGPASSWORD = $DB_PASSWORD

Write-Host "Подключение к БД: $DB_HOST:$DB_PORT/$DB_NAME" -ForegroundColor Yellow
Write-Host ""

# 1. Проверка клиента noteapp
Write-Host "1. Проверка регистрации клиента noteapp:" -ForegroundColor Green
$query1 = @"
SELECT 
    c.id AS client_id,
    c.name AS client_name,
    c.description,
    substring(c.api_key, 1, 20) || '...' AS api_key_preview,
    c.is_active,
    c.created_at
FROM clients c
WHERE c.name = 'noteapp';
"@
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query1
Write-Host ""

# 2. Проверка активных нейросетей
Write-Host "2. Доступные нейросети в AI Integration Service:" -ForegroundColor Green
$query2 = @"
SELECT 
    n.name AS network_name,
    n.display_name,
    n.network_type,
    n.provider,
    n.is_free,
    n.priority,
    n.is_active
FROM neural_networks n
WHERE n.is_active = true
ORDER BY n.priority DESC;
"@
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query2
Write-Host ""

# 3. Проверка связей noteapp с нейросетями
Write-Host "3. Связи noteapp с нейросетями (client_network_access):" -ForegroundColor Green
$query3 = @"
SELECT 
    c.name AS client,
    n.name AS network_name,
    n.network_type,
    cna.has_access,
    cna.daily_limit,
    cna.monthly_limit,
    cna.remaining_requests_today,
    cna.remaining_requests_month
FROM client_network_access cna
JOIN clients c ON cna.client_id = c.id
JOIN neural_networks n ON cna.network_id = n.id
WHERE c.name = 'noteapp'
ORDER BY n.priority DESC;
"@
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query3
Write-Host ""

# 4. Нейросети БЕЗ связей с noteapp
Write-Host "4. Нейросети БЕЗ доступа для noteapp (должно быть пусто!):" -ForegroundColor Yellow
$query4 = @"
SELECT 
    n.name AS network_name,
    n.display_name,
    n.network_type,
    n.priority
FROM neural_networks n
WHERE n.is_active = true
  AND NOT EXISTS (
    SELECT 1 
    FROM client_network_access cna
    JOIN clients c ON cna.client_id = c.id
    WHERE cna.network_id = n.id 
      AND c.name = 'noteapp'
      AND cna.has_access = true
  )
ORDER BY n.priority DESC;
"@
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query4
Write-Host ""

# 5. Статистика
Write-Host "5. Статистика:" -ForegroundColor Green
$query5 = @"
SELECT 
    (SELECT COUNT(*) FROM neural_networks WHERE is_active = true) AS total_active_networks,
    (SELECT COUNT(*) FROM clients WHERE name = 'noteapp') AS noteapp_registered,
    (SELECT COUNT(*) 
     FROM client_network_access cna
     JOIN clients c ON cna.client_id = c.id
     WHERE c.name = 'noteapp' AND cna.has_access = true) AS noteapp_networks_with_access;
"@
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query5
Write-Host ""

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "Проверка завершена!" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Очистка переменной окружения с паролем
$env:PGPASSWORD = $null

