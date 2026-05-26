-- WibeStyle try-on: Grok Imagine image edit (person + garment reference photos)

UPDATE neural_networks
SET
    provider = 'virtual_try_on',
    network_type = 'image_generation',
    api_url = 'https://api.x.ai/v1',
    model_name = 'grok-imagine-image-quality',
    request_mapping = '{"tryOnBackend":"grok-imagine-edit"}',
    timeout_seconds = 180,
    max_retries = 2,
    updated_at = NOW()
WHERE name = 'wibestyle-vton';
