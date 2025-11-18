-- Создание таблицы для хранения пользовательских API ключей нейросетей
-- Ключи хранятся в связке: пользователь - сервис - нейросеть

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS public.user_api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_account_id UUID NOT NULL,
    client_application_id UUID NOT NULL,
    neural_network_id UUID NOT NULL,
    api_key_encrypted TEXT NOT NULL, -- Зашифрованный API ключ
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_user_api_keys_user_account FOREIGN KEY (user_account_id) REFERENCES public.user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_api_keys_client_application FOREIGN KEY (client_application_id) REFERENCES public.client_applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_api_keys_neural_network FOREIGN KEY (neural_network_id) REFERENCES public.neural_networks(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_api_keys_unique UNIQUE (user_account_id, client_application_id, neural_network_id)
);

-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_user_api_keys_user_account_id ON public.user_api_keys(user_account_id);
CREATE INDEX IF NOT EXISTS idx_user_api_keys_client_application_id ON public.user_api_keys(client_application_id);
CREATE INDEX IF NOT EXISTS idx_user_api_keys_neural_network_id ON public.user_api_keys(neural_network_id);
CREATE INDEX IF NOT EXISTS idx_user_api_keys_user_client_network ON public.user_api_keys(user_account_id, client_application_id, neural_network_id);

-- Комментарии
COMMENT ON TABLE public.user_api_keys IS 'Пользовательские API ключи для нейросетей (доступно только для платного тарифа)';
COMMENT ON COLUMN public.user_api_keys.api_key_encrypted IS 'Зашифрованный API ключ пользователя';

-- Триггер для автоматического обновления updated_at
DROP TRIGGER IF EXISTS update_user_api_keys_updated_at ON public.user_api_keys;
CREATE TRIGGER update_user_api_keys_updated_at
BEFORE UPDATE ON public.user_api_keys
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

