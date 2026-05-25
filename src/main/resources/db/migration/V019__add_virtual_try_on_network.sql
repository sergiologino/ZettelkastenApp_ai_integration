-- Virtual try-on network for WibeStyle (Pollinations-backed image generation with garment/person context)

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'wibestyle-vton',
    'WibeStyle Virtual Try-On',
    'virtual_try_on',
    'image_generation',
    'https://api.pollinations.ai/v1/images',
    '',
    'pollinations-lite',
    true,
    true,
    45,
    120,
    2,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    provider = EXCLUDED.provider,
    network_type = EXCLUDED.network_type,
    api_url = EXCLUDED.api_url,
    model_name = EXCLUDED.model_name,
    is_active = EXCLUDED.is_active,
    is_free = EXCLUDED.is_free,
    updated_at = NOW();
