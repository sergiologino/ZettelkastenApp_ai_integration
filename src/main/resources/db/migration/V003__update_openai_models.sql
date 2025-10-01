-- Update OpenAI models to current versions

-- Update gpt-4 to gpt-4o
UPDATE neural_networks 
SET 
    name = 'openai-gpt4o',
    display_name = 'OpenAI GPT-4o',
    model_name = 'gpt-4o',
    updated_at = NOW()
WHERE name = 'openai-gpt4';

-- Update gpt-3.5-turbo name for consistency
UPDATE neural_networks 
SET 
    name = 'openai-gpt35-turbo',
    display_name = 'OpenAI GPT-3.5 Turbo',
    updated_at = NOW()
WHERE name = 'openai-gpt35';

-- Insert GPT-4o-mini if not exists
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'openai-gpt4o-mini',
    'OpenAI GPT-4o Mini',
    'openai',
    'chat',
    'https://api.openai.com/v1',
    '',
    'gpt-4o-mini',
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

-- Add network limits for the new gpt-4o-mini model

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
WHERE nn.name = 'openai-gpt4o-mini'
ON CONFLICT DO NOTHING;

-- Limits for free users
INSERT INTO network_limits (id, neural_network_id, user_type, limit_period, request_limit, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    nn.id,
    'free_user',
    'daily',
    20,
    NOW(),
    NOW()
FROM neural_networks nn
WHERE nn.name = 'openai-gpt4o-mini'
ON CONFLICT DO NOTHING;

-- Limits for paid users (100 requests/month)
INSERT INTO network_limits (id, neural_network_id, user_type, limit_period, request_limit, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    nn.id,
    'paid_user',
    'monthly',
    100,
    NOW(),
    NOW()
FROM neural_networks nn
WHERE nn.name = 'openai-gpt4o-mini'
ON CONFLICT DO NOTHING;

