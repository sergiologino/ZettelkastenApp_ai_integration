-- Создание системы подписок для AI Integration Service

-- 1. Таблица тарифных планов
CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19,2) NOT NULL CHECK (price >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'RUB',
    duration_days INTEGER NOT NULL CHECK (duration_days >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Таблица подписок пользователей
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_account_id UUID NOT NULL,
    subscription_plan_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED')),
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_subscriptions_user_account FOREIGN KEY (user_account_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plans(id)
);

-- 3. Таблица истории платежей
CREATE TABLE IF NOT EXISTS payment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_account_id UUID NOT NULL,
    subscription_id UUID,
    subscription_plan_id UUID NOT NULL,
    amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'RUB',
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CARD', 'MOBILE_PAYMENT', 'BANK_TRANSFER', 'E_WALLET')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    provider_transaction_id VARCHAR(255),
    provider_name VARCHAR(100) DEFAULT 'YooKassa',
    payment_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    failure_reason TEXT,
    webhook_data TEXT,
    refunded_at TIMESTAMP,
    refund_reason TEXT,
    
    CONSTRAINT fk_payment_history_user FOREIGN KEY (user_account_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_history_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    CONSTRAINT fk_payment_history_plan FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plans(id)
);

-- 4. Добавление полей в user_accounts
ALTER TABLE user_accounts 
ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(20) DEFAULT 'FREE' CHECK (subscription_status IN ('FREE', 'ACTIVE', 'EXPIRED', 'CANCELLED')),
ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMP;

-- 5. Добавление free_request_limit в client_network_access
ALTER TABLE client_network_access 
ADD COLUMN IF NOT EXISTS free_request_limit INTEGER CHECK (free_request_limit >= 0);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_subscription_plans_name ON subscription_plans(name);
CREATE INDEX IF NOT EXISTS idx_subscription_plans_active ON subscription_plans(is_active);
CREATE INDEX IF NOT EXISTS idx_subscription_plans_sort_order ON subscription_plans(sort_order);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_account_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_plan_id ON subscriptions(subscription_plan_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_expires_at ON subscriptions(expires_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status ON subscriptions(user_account_id, status);

CREATE INDEX IF NOT EXISTS idx_payment_history_user_id ON payment_history(user_account_id);
CREATE INDEX IF NOT EXISTS idx_payment_history_subscription_id ON payment_history(subscription_id);
CREATE INDEX IF NOT EXISTS idx_payment_history_transaction_id ON payment_history(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_history_provider_transaction_id ON payment_history(provider_transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_history_status ON payment_history(status);

CREATE INDEX IF NOT EXISTS idx_user_accounts_subscription_status ON user_accounts(subscription_status);
CREATE INDEX IF NOT EXISTS idx_user_accounts_subscription_expires_at ON user_accounts(subscription_expires_at);

-- Триггеры для updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_subscription_plans_updated_at ON subscription_plans;
CREATE TRIGGER update_subscription_plans_updated_at
BEFORE UPDATE ON subscription_plans
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_subscriptions_updated_at ON subscriptions;
CREATE TRIGGER update_subscriptions_updated_at
BEFORE UPDATE ON subscriptions
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Вставка базовых тарифных планов
INSERT INTO subscription_plans (name, display_name, description, price, currency, duration_days, sort_order) VALUES 
('FREE', 'Бесплатный план', 'Доступ к бесплатным нейросетям и ограниченное количество запросов к платным', 0.00, 'RUB', 0, 1),
('PAID', 'Платный план', 'Полный доступ ко всем нейросетям с лимитами из настроек', 999.00, 'RUB', 30, 2)
ON CONFLICT (name) DO NOTHING;

-- Комментарии
COMMENT ON TABLE subscription_plans IS 'Тарифные планы подписок';
COMMENT ON TABLE subscriptions IS 'Активные подписки пользователей';
COMMENT ON TABLE payment_history IS 'История платежей пользователей';
COMMENT ON COLUMN client_network_access.free_request_limit IS 'Лимит бесплатных запросов для бесплатного плана (NULL = без лимита для бесплатного плана)';

