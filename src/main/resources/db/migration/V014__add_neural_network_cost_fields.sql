-- Добавление полей себестоимости и метрик для нейросетей

ALTER TABLE neural_networks 
ADD COLUMN IF NOT EXISTS cost_per_token_rub DECIMAL(19,8) DEFAULT 0.0;

ALTER TABLE neural_networks 
ADD COLUMN IF NOT EXISTS words_per_token DECIMAL(10,4);

ALTER TABLE neural_networks 
ADD COLUMN IF NOT EXISTS seconds_per_token DECIMAL(10,4);

-- Комментарии
COMMENT ON COLUMN neural_networks.cost_per_token_rub IS 'Себестоимость одного токена в рублях (курс: 1 USD = 90 RUB)';
COMMENT ON COLUMN neural_networks.words_per_token IS 'Примерное количество слов в одном токене (для текстовых моделей)';
COMMENT ON COLUMN neural_networks.seconds_per_token IS 'Примерное количество секунд в одном токене (для транскрибации)';

-- Индекс для производительности
CREATE INDEX IF NOT EXISTS idx_neural_networks_cost ON neural_networks(cost_per_token_rub);

