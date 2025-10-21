-- Проверка администратора в базе данных
SELECT 
    id,
    username,
    email,
    password_hash,
    is_active,
    created_at
FROM admin_users
WHERE username = 'admin';

-- Если администратора нет, показать всех админов
SELECT COUNT(*) as admin_count FROM admin_users;

