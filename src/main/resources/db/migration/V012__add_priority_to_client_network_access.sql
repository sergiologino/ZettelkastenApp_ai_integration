-- Добавление поля priority для управления порядком нейросетей в client_network_access

ALTER TABLE client_network_access 
ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 100;

-- Создаем индекс для сортировки по приоритету
CREATE INDEX IF NOT EXISTS idx_client_network_access_priority ON client_network_access (client_application_id, priority);

-- Комментарий
COMMENT ON COLUMN client_network_access.priority IS 'Приоритет нейросети для клиента (меньше = выше приоритет, используется при автовыборе нейросети)';

