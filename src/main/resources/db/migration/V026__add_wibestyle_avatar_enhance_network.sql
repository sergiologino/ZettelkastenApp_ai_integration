-- Opt-in WibeStyle avatar enhancement. Reuses the existing encrypted OpenAI key
-- from the GPT-4o-mini network; no plaintext secret is stored in this migration.
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    'wibestyle-avatar-enhance',
    'WibeStyle Avatar Enhance (GPT Image 1.5)',
    'openai',
    'image_edit',
    'https://api.openai.com/v1/images',
    COALESCE((SELECT api_key_encrypted FROM neural_networks WHERE name = 'openai-gpt4o-mini'), ''),
    'gpt-image-1.5',
    true,
    false,
    12,
    180,
    1,
    '{"operation":"avatar_enhance","inputFidelity":"high"}'::jsonb,
    '{}'::jsonb,
    NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM neural_networks WHERE name = 'wibestyle-avatar-enhance');

UPDATE neural_networks
SET
    display_name = 'WibeStyle Avatar Enhance (GPT Image 1.5)',
    provider = 'openai',
    network_type = 'image_edit',
    api_url = 'https://api.openai.com/v1/images',
    api_key_encrypted = CASE
        WHEN api_key_encrypted IS NULL OR api_key_encrypted = ''
            THEN COALESCE((SELECT api_key_encrypted FROM neural_networks WHERE name = 'openai-gpt4o-mini'), '')
        ELSE api_key_encrypted
    END,
    model_name = 'gpt-image-1.5',
    is_active = true,
    is_free = false,
    priority = 12,
    timeout_seconds = 180,
    max_retries = 1,
    request_mapping = '{"operation":"avatar_enhance","inputFidelity":"high"}'::jsonb,
    updated_at = NOW()
WHERE name = 'wibestyle-avatar-enhance';
