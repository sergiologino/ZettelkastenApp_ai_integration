INSERT INTO neural_networks (
    id,
    name,
    display_name,
    provider,
    network_type,
    api_url,
    model_name,
    is_active,
    priority,
    request_mapping,
    response_mapping,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'wibestyle-season-video',
    'WibeStyle Season Hit Video',
    'season_hit_video',
    'video_generation',
    'https://api.x.ai/v1/videos/generations',
    'grok-imagine-video',
    true,
    10,
    '{"tryOnBackend":"grok-video"}'::jsonb,
    '{}'::jsonb,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM neural_networks WHERE name = 'wibestyle-season-video'
);
