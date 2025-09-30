-- Initial schema for AI Integration Service

-- Таблица клиентских приложений
CREATE TABLE IF NOT EXISTS client_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Таблица настроек нейросетей
CREATE TABLE IF NOT EXISTS neural_networks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL, -- openai, yandex, anthropic, mistral, sber, etc.
    network_type VARCHAR(50) NOT NULL, -- chat, transcription, embedding, etc.
    api_url TEXT NOT NULL,
    api_key_encrypted TEXT,
    model_name VARCHAR(100), -- gpt-4, claude-3, etc.
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_free BOOLEAN NOT NULL DEFAULT FALSE, -- бесплатная ли нейросеть
    priority INTEGER NOT NULL DEFAULT 100, -- приоритет использования (меньше = выше приоритет)
    timeout_seconds INTEGER DEFAULT 60,
    max_retries INTEGER DEFAULT 3,
    request_mapping JSONB, -- маппинг полей запроса
    response_mapping JSONB, -- маппинг полей ответа
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Таблица лимитов для нейросетей
CREATE TABLE IF NOT EXISTS network_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neural_network_id UUID NOT NULL REFERENCES neural_networks(id) ON DELETE CASCADE,
    user_type VARCHAR(50) NOT NULL, -- new_user, free_user, paid_user
    limit_period VARCHAR(50) NOT NULL, -- daily, monthly, yearly
    request_limit INTEGER, -- NULL = unlimited
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(neural_network_id, user_type, limit_period)
);

-- Таблица пользователей из клиентских приложений
CREATE TABLE IF NOT EXISTS external_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_app_id UUID NOT NULL REFERENCES client_applications(id) ON DELETE CASCADE,
    external_user_id VARCHAR(255) NOT NULL, -- ID пользователя в клиентском приложении
    user_type VARCHAR(50) NOT NULL DEFAULT 'free_user', -- new_user, free_user, paid_user
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(client_app_id, external_user_id)
);

-- Таблица счётчиков использования для пользователей
CREATE TABLE IF NOT EXISTS usage_counters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_user_id UUID NOT NULL REFERENCES external_users(id) ON DELETE CASCADE,
    neural_network_id UUID NOT NULL REFERENCES neural_networks(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    token_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(external_user_id, neural_network_id, period_start)
);

-- Индекс для быстрого поиска актуальных счётчиков
CREATE INDEX idx_usage_counters_user_network_period ON usage_counters(external_user_id, neural_network_id, period_start, period_end);

-- Таблица логов запросов
CREATE TABLE IF NOT EXISTS request_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_app_id UUID NOT NULL REFERENCES client_applications(id) ON DELETE CASCADE,
    external_user_id UUID REFERENCES external_users(id) ON DELETE SET NULL,
    neural_network_id UUID REFERENCES neural_networks(id) ON DELETE SET NULL,
    request_type VARCHAR(50) NOT NULL, -- chat, transcription, etc.
    request_payload JSONB NOT NULL,
    response_payload JSONB,
    status VARCHAR(50) NOT NULL, -- pending, success, failed, rate_limited
    error_message TEXT,
    execution_time_ms INTEGER,
    tokens_used INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Индексы для быстрого поиска логов
CREATE INDEX idx_request_logs_client_created ON request_logs(client_app_id, created_at DESC);
CREATE INDEX idx_request_logs_user_created ON request_logs(external_user_id, created_at DESC);
CREATE INDEX idx_request_logs_network_created ON request_logs(neural_network_id, created_at DESC);
CREATE INDEX idx_request_logs_status ON request_logs(status);

-- Таблица для администраторов AI-сервиса
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Таблица настроек системы
CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(100) NOT NULL UNIQUE,
    value TEXT,
    description TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by UUID REFERENCES admin_users(id) ON DELETE SET NULL
);

-- Вставляем дефолтные настройки
INSERT INTO system_settings (id, key, value, description) VALUES
(gen_random_uuid(), 'default_timeout', '60', 'Default timeout for AI requests in seconds'),
(gen_random_uuid(), 'max_retries', '3', 'Maximum number of retries for failed requests'),
(gen_random_uuid(), 'enable_fallback', 'true', 'Enable automatic fallback to free networks when limit reached');

-- Комментарии к таблицам
COMMENT ON TABLE client_applications IS 'Клиентские приложения, которые используют AI-сервис';
COMMENT ON TABLE neural_networks IS 'Настройки подключённых нейросетей';
COMMENT ON TABLE network_limits IS 'Лимиты запросов для разных типов пользователей';
COMMENT ON TABLE external_users IS 'Пользователи из клиентских приложений';
COMMENT ON TABLE usage_counters IS 'Счётчики использования AI для пользователей';
COMMENT ON TABLE request_logs IS 'Логи всех запросов к AI';
COMMENT ON TABLE admin_users IS 'Администраторы AI-сервиса';
COMMENT ON TABLE system_settings IS 'Общие настройки системы';

