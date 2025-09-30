-- Seed data for testing and initial setup

-- Insert first admin user (password: admin123)
-- Password hash for 'admin123' using BCrypt
INSERT INTO admin_users (id, username, password_hash, email, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8i7vLw4bWPnDEp1nce',
    'admin@example.com',
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

-- Insert sample client application
INSERT INTO client_applications (id, name, description, api_key, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'noteapp',
    'AltaNote application',
    'aikey_sample_noteapp_key_replace_in_production',
    true,
    NOW(),
    NOW()
) ON CONFLICT (api_key) DO NOTHING;

-- Insert sample neural networks

-- OpenAI GPT-4
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'openai-gpt4',
    'OpenAI GPT-4',
    'openai',
    'chat',
    'https://api.openai.com/v1',
    '', -- API key will be added by admin
    'gpt-4',
    false, -- Inactive until API key is set
    false,
    10,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- OpenAI GPT-3.5 Turbo (cheaper option)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'openai-gpt35',
    'OpenAI GPT-3.5 Turbo',
    'openai',
    'chat',
    'https://api.openai.com/v1',
    '',
    'gpt-3.5-turbo',
    false,
    false,
    20,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Yandex GPT
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'yandex-gpt-lite',
    'Yandex GPT Lite',
    'yandex',
    'chat',
    'https://llm.api.cloud.yandex.net',
    '',
    'gpt://folder_id/yandexgpt-lite/latest',
    false,
    true, -- Free tier available
    100,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Claude
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'claude-3-opus',
    'Claude 3 Opus',
    'anthropic',
    'chat',
    'https://api.anthropic.com',
    '',
    'claude-3-opus-20240229',
    false,
    false,
    15,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Mistral
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'mistral-large',
    'Mistral Large',
    'mistral',
    'chat',
    'https://api.mistral.ai',
    '',
    'mistral-large-latest',
    false,
    false,
    25,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- GigaChat
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'gigachat',
    'GigaChat',
    'sber',
    'chat',
    'https://gigachat.devices.sberbank.ru/api',
    '',
    'GigaChat',
    false,
    true,
    90,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Whisper for transcription
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'whisper',
    'Whisper (Audio Transcription)',
    'whisper',
    'transcription',
    'https://api.openai.com/v1',
    '',
    'whisper-1',
    false,
    false,
    10,
    120,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Insert default limits for networks

-- Limits for new users (10 requests/day)
INSERT INTO network_limits (id, neural_network_id, user_type, limit_period, request_limit, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    nn.id,
    'new_user',
    'daily',
    10,
    NOW(),
    NOW()
FROM neural_networks nn
WHERE nn.is_free = false
ON CONFLICT DO NOTHING;

-- Limits for free users (higher limits on free networks)
INSERT INTO network_limits (id, neural_network_id, user_type, limit_period, request_limit, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    nn.id,
    'free_user',
    'daily',
    CASE 
        WHEN nn.is_free THEN 50
        ELSE 20
    END,
    NOW(),
    NOW()
FROM neural_networks nn
ON CONFLICT DO NOTHING;

-- Limits for paid users (100 requests/month on paid networks, unlimited on free)
INSERT INTO network_limits (id, neural_network_id, user_type, limit_period, request_limit, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    nn.id,
    'paid_user',
    'monthly',
    CASE 
        WHEN nn.is_free THEN NULL -- Unlimited
        ELSE 100
    END,
    NOW(),
    NOW()
FROM neural_networks nn
ON CONFLICT DO NOTHING;

-- Insert default system settings (already done in V001, but ensure they exist)
INSERT INTO system_settings (id, key, value, description)
VALUES
    (gen_random_uuid(), 'maintenance_mode', 'false', 'Enable/disable maintenance mode'),
    (gen_random_uuid(), 'default_network_type', 'chat', 'Default network type for auto-selection')
ON CONFLICT (key) DO NOTHING;

