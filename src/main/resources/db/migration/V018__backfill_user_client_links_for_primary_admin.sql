-- Клиенты API без записи в user_client_links ломали лимиты в старых версиях (ошибка «не привязано к пользователю»).
-- Создаём user_accounts для email основного admin_users (если ещё нет) и связываем все активные client_applications.

INSERT INTO user_accounts (id, email, full_name, provider, is_active, created_at, updated_at, subscription_status)
SELECT gen_random_uuid(),
       a.email,
       a.username,
       'migration-bootstrap',
       true,
       NOW(),
       NOW(),
       'FREE'
FROM (
    SELECT email, username
    FROM admin_users
    ORDER BY CASE WHEN username = 'admin' THEN 0 ELSE 1 END, created_at ASC
    LIMIT 1
) a
WHERE NOT EXISTS (SELECT 1 FROM user_accounts ua WHERE ua.email = a.email);

INSERT INTO user_client_links (id, user_id, client_application_id, created_at)
SELECT gen_random_uuid(),
       ua.id,
       ca.id,
       NOW()
FROM client_applications ca
JOIN user_accounts ua
  ON ua.email = (
      SELECT a.email
      FROM admin_users a
      ORDER BY CASE WHEN a.username = 'admin' THEN 0 ELSE 1 END, a.created_at ASC
      LIMIT 1
  )
WHERE ca.deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM user_client_links ucl WHERE ucl.client_application_id = ca.id
  );
