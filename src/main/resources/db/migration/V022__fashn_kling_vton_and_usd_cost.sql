-- FASHN / Kling virtual try-on networks, USD cost fields, request log cost

ALTER TABLE neural_networks
ADD COLUMN IF NOT EXISTS cost_per_token_usd DECIMAL(19,8) DEFAULT 0.0;

ALTER TABLE request_logs
ADD COLUMN IF NOT EXISTS cost_usd DECIMAL(19,8);

COMMENT ON COLUMN neural_networks.cost_per_token_usd IS 'Cost of one token/credit unit in USD';
COMMENT ON COLUMN request_logs.cost_usd IS 'Estimated request cost in USD at log time';

-- FASHN product-to-model (generates model wearing product from flat-lay)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, cost_per_token_usd, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'fashn-product-to-model',
    'FASHN Product to Model',
    'fashn',
    'image_generation',
    'https://api.fashn.ai/v1',
    '',
    'product-to-model',
    false,
    false,
    50,
    180,
    2,
    '{"tryOnBackend":"fashn-product-to-model","fashnModel":"product-to-model","creditsPerRequest":2}'::jsonb,
    '{}'::jsonb,
    0.02000000,
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    provider = EXCLUDED.provider,
    network_type = EXCLUDED.network_type,
    api_url = EXCLUDED.api_url,
    model_name = EXCLUDED.model_name,
    request_mapping = EXCLUDED.request_mapping,
    cost_per_token_usd = EXCLUDED.cost_per_token_usd,
    timeout_seconds = EXCLUDED.timeout_seconds,
    updated_at = NOW();

-- FASHN tryon-max (person + garment virtual try-on)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, cost_per_token_usd, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'fashn-tryon-max',
    'FASHN Try-On Max',
    'fashn',
    'image_generation',
    'https://api.fashn.ai/v1',
    '',
    'tryon-max',
    false,
    false,
    48,
    180,
    2,
    '{"tryOnBackend":"fashn-tryon-max","fashnModel":"tryon-max","creditsPerRequest":2}'::jsonb,
    '{}'::jsonb,
    0.02000000,
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    provider = EXCLUDED.provider,
    network_type = EXCLUDED.network_type,
    api_url = EXCLUDED.api_url,
    model_name = EXCLUDED.model_name,
    request_mapping = EXCLUDED.request_mapping,
    cost_per_token_usd = EXCLUDED.cost_per_token_usd,
    timeout_seconds = EXCLUDED.timeout_seconds,
    updated_at = NOW();

-- Kling Kolors Virtual Try-On v1.5
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, cost_per_token_usd, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'kling-kolors-tryon',
    'Kling Kolors Virtual Try-On',
    'kling',
    'image_generation',
    'https://api.klingai.com',
    '',
    'kolors-virtual-try-on-v1-5',
    false,
    false,
    47,
    180,
    2,
    '{"tryOnBackend":"kling-kolors-tryon","tokensPerRequest":1}'::jsonb,
    '{}'::jsonb,
    0.07000000,
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    provider = EXCLUDED.provider,
    network_type = EXCLUDED.network_type,
    api_url = EXCLUDED.api_url,
    model_name = EXCLUDED.model_name,
    request_mapping = EXCLUDED.request_mapping,
    cost_per_token_usd = EXCLUDED.cost_per_token_usd,
    timeout_seconds = EXCLUDED.timeout_seconds,
    updated_at = NOW();
