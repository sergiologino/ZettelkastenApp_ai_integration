-- Добавляем поле для инструкции по подключению к нейросети
ALTER TABLE neural_networks 
ADD COLUMN connection_instruction TEXT;

COMMENT ON COLUMN neural_networks.connection_instruction IS 'Подробная инструкция с эндпоинтами и ответами для подключения клиентов к нейросети';

