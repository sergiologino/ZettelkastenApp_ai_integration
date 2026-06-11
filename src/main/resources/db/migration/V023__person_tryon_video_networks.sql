-- Person+garment try-on video networks (FASHN tryon-max → image-to-video, Kling try-on → image2video)

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, cost_per_token_usd, connection_instruction, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'fashn-tryon-video',
    'FASHN Try-On Video (person + garment)',
    'fashn',
    'video_generation',
    'https://api.fashn.ai/v1',
    '',
    'tryon-max',
    false,
    false,
    46,
    300,
    2,
    '{"pipeline":"person-tryon-video","tryOnBackend":"fashn-tryon-video","fashnModel":"tryon-max","videoCreditsPerRequest":3,"fashnDefaults":{"resolution":"720p","duration":5}}'::jsonb,
    '{}'::jsonb,
    0.02500000,
    'Person try-on video: payload personImageBase64 + garmentImageBase64 (+ optional prompt, durationSec, videoResolution). Pipeline: tryon-max then image-to-video.',
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    provider = EXCLUDED.provider,
    network_type = EXCLUDED.network_type,
    api_url = EXCLUDED.api_url,
    model_name = EXCLUDED.model_name,
    request_mapping = EXCLUDED.request_mapping,
    cost_per_token_usd = EXCLUDED.cost_per_token_usd,
    connection_instruction = EXCLUDED.connection_instruction,
    timeout_seconds = EXCLUDED.timeout_seconds,
    updated_at = NOW();

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, cost_per_token_usd, connection_instruction, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'kling-tryon-video',
    'Kling Try-On Video (person + garment)',
    'kling',
    'video_generation',
    'https://api.klingai.com',
    '',
    'kolors-virtual-try-on-v1-5',
    false,
    false,
    45,
    300,
    2,
    '{"pipeline":"person-tryon-video","tryOnBackend":"kling-tryon-video","videoModel":"kling-v1-6","tryOnTokensPerRequest":1,"videoTokensPerRequest":1}'::jsonb,
    '{}'::jsonb,
    0.14000000,
    'Person try-on video: payload personImageBase64 + garmentImageBase64 (+ optional prompt/motionPrompt, durationSec). Pipeline: kolors-virtual-try-on then image2video.',
    NOW(),
    NOW()
) ON CONFLICT (name) DO UPDATE SET
    provider = EXCLUDED.provider,
    network_type = EXCLUDED.network_type,
    api_url = EXCLUDED.api_url,
    model_name = EXCLUDED.model_name,
    request_mapping = EXCLUDED.request_mapping,
    cost_per_token_usd = EXCLUDED.cost_per_token_usd,
    connection_instruction = EXCLUDED.connection_instruction,
    timeout_seconds = EXCLUDED.timeout_seconds,
    updated_at = NOW();

UPDATE neural_networks
SET
    display_name = 'FASHN Try-On Photo (person + garment)',
    connection_instruction = 'Photo try-on on YOUR person: personImageBase64 + garmentImageBase64. Uses tryon-max (not product-to-model).',
    request_mapping = '{"tryOnBackend":"fashn-tryon-max","fashnModel":"tryon-max","creditsPerRequest":2}'::jsonb,
    updated_at = NOW()
WHERE name = 'fashn-tryon-max';

UPDATE neural_networks
SET
    display_name = 'Kling Try-On Photo (person + garment)',
    connection_instruction = 'Photo try-on: personImageBase64 + garmentImageBase64. Kolors Virtual Try-On v1.5.',
    updated_at = NOW()
WHERE name = 'kling-kolors-tryon';
