-- V017: OpenAI TTS и Yandex SpeechKit TTS (тип сети speech_synthesis)
-- Ключи провайдеров пустые; is_active=false до настройки в админке.

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'openai-tts',
    'OpenAI TTS',
    'openai',
    'speech_synthesis',
    'https://api.openai.com/v1',
    '',
    'tts-1',
    false,
    false,
    40,
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
) VALUES (
    gen_random_uuid(),
    'yandex-speechkit-tts',
    'Yandex SpeechKit TTS',
    'yandex',
    'speech_synthesis',
    'https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize',
    '',
    NULL,
    false,
    false,
    41,
    120,
    2,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;
