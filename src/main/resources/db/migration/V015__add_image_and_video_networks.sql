-- Add image and video generation providers

-- Image generation networks
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'pollinations-lite',
    'Pollinations Lite (Fast Diffusion)',
    'pollinations',
    'image_generation',
    'https://api.pollinations.ai/v1/images',
    '',
    'pollinations-lite',
    false,
    true,
    40,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'stability-sd3',
    'Stability SD3 Turbo',
    'stability',
    'image_generation',
    'https://api.stability.ai/v2beta/image/text-to-image',
    '',
    'sd3-turbo',
    false,
    false,
    35,
    90,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'leonardo-phoenix',
    'Leonardo Phoenix (4K Illustration)',
    'leonardo',
    'image_generation',
    'https://cloud.leonardo.ai/api/rest/v1/generations',
    '',
    'phoenix',
    false,
    false,
    30,
    90,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'midjourney-v6',
    'Midjourney V6',
    'midjourney',
    'image_generation',
    'https://api.midjourney.com/v1/jobs',
    '',
    'midjourney-v6',
    false,
    false,
    25,
    120,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'openai-dalle3',
    'OpenAI DALL·E 3',
    'openai',
    'image_generation',
    'https://api.openai.com/v1/images',
    '',
    'dall-e-3',
    false,
    false,
    20,
    120,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Video generation networks
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'sora2-creative',
    'OpenAI Sora 2 Creative',
    'sora2',
    'video_generation',
    'https://video.api.openai.com/v1/sora',
    '',
    'sora-2',
    false,
    false,
    18,
    180,
    2,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'nanobanana-studio',
    'Nano Banana Studio (Realtime Video)',
    'nanobanana',
    'video_generation',
    'https://api.nanobanana.ai/v1/videos',
    '',
    'nanobanana-studio',
    false,
    false,
    22,
    120,
    2,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'runway-gen3',
    'Runway Gen-3 Alpha',
    'runwayml',
    'video_generation',
    'https://api.runwayml.com/v1/videos',
    '',
    'gen-3-alpha',
    false,
    false,
    24,
    150,
    2,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'pika-15',
    'Pika 1.5 Ultra',
    'pika',
    'video_generation',
    'https://api.pika.art/v1/videos',
    '',
    'pika-1-5-ultra',
    false,
    false,
    26,
    150,
    2,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;


