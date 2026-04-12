-- V016: Add new AI models (March 2026 catalog update)
-- OpenAI GPT-5.4 family, GPT-4.1 family, o-series reasoning,
-- Anthropic Claude 4.5/4.6 family, Google Gemini 2.5, xAI Grok 3,
-- DeepSeek R1, Mistral Large 3

-- =============================================
-- OpenAI: GPT-5.4 (released March 5 2026)
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'openai-gpt54', 'OpenAI GPT-5.4', 'openai', 'chat',
    'https://api.openai.com/v1', '', 'gpt-5.4',
    false, false, 2, 90, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'openai-gpt54-pro', 'OpenAI GPT-5.4 Pro', 'openai', 'chat',
    'https://api.openai.com/v1', '', 'gpt-5.4-pro',
    false, false, 3, 120, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

-- =============================================
-- OpenAI: GPT-4.1 family (released April 2025)
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'openai-gpt41', 'OpenAI GPT-4.1', 'openai', 'chat',
    'https://api.openai.com/v1', '', 'gpt-4.1',
    false, false, 9, 60, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'openai-gpt41-mini', 'OpenAI GPT-4.1 Mini', 'openai', 'chat',
    'https://api.openai.com/v1', '', 'gpt-4.1-mini',
    false, false, 11, 60, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'openai-gpt41-nano', 'OpenAI GPT-4.1 Nano', 'openai', 'chat',
    'https://api.openai.com/v1', '', 'gpt-4.1-nano',
    false, false, 12, 30, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

-- =============================================
-- OpenAI: o-series reasoning models (2025)
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'openai-o3', 'OpenAI o3 (Reasoning)', 'openai', 'chat',
    'https://api.openai.com/v1', '', 'o3',
    false, false, 6, 120, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'openai-o3-pro', 'OpenAI o3 Pro (Deep Reasoning)', 'openai', 'chat',
    'https://api.openai.com/v1', '', 'o3-pro',
    false, false, 7, 180, 2, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'openai-o4-mini', 'OpenAI o4-mini (Fast Reasoning)', 'openai', 'chat',
    'https://api.openai.com/v1', '', 'o4-mini',
    false, false, 8, 90, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

-- =============================================
-- Anthropic: Claude 4.6 (released February 2026)
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'claude-opus-46', 'Claude Opus 4.6', 'anthropic', 'chat',
    'https://api.anthropic.com', '', 'claude-opus-4-6',
    false, false, 4, 90, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'claude-sonnet-46', 'Claude Sonnet 4.6', 'anthropic', 'chat',
    'https://api.anthropic.com', '', 'claude-sonnet-4-6',
    false, false, 5, 60, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

-- =============================================
-- Anthropic: Claude 4.5 (released late 2025)
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'claude-opus-45', 'Claude Opus 4.5', 'anthropic', 'chat',
    'https://api.anthropic.com', '', 'claude-opus-4-5-20251101',
    false, false, 13, 90, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'claude-sonnet-45', 'Claude Sonnet 4.5', 'anthropic', 'chat',
    'https://api.anthropic.com', '', 'claude-sonnet-4-5-20250929',
    false, false, 14, 60, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'claude-haiku-45', 'Claude Haiku 4.5 (Fast)', 'anthropic', 'chat',
    'https://api.anthropic.com', '', 'claude-haiku-4-5-20251001',
    false, false, 17, 30, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

-- =============================================
-- Google: Gemini 2.5 (GA June 2025, OpenAI-compatible API)
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'gemini-25-pro', 'Google Gemini 2.5 Pro', 'google', 'chat',
    'https://generativelanguage.googleapis.com/v1beta/openai', '', 'gemini-2.5-pro',
    false, false, 10, 90, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'gemini-25-flash', 'Google Gemini 2.5 Flash', 'google', 'chat',
    'https://generativelanguage.googleapis.com/v1beta/openai', '', 'gemini-2.5-flash',
    false, false, 16, 60, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

-- =============================================
-- xAI: Grok 3 (2025, OpenAI-compatible API)
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'grok-3', 'xAI Grok 3', 'xai', 'chat',
    'https://api.x.ai/v1', '', 'grok-3',
    false, false, 20, 90, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'grok-3-mini', 'xAI Grok 3 Mini (Fast)', 'xai', 'chat',
    'https://api.x.ai/v1', '', 'grok-3-mini',
    false, false, 22, 60, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

-- =============================================
-- DeepSeek: R1 Reasoner
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'deepseek-reasoner', 'DeepSeek R1 (Reasoner)', 'deepseek', 'chat',
    'https://api.deepseek.com/v1', '', 'deepseek-reasoner',
    false, false, 15, 120, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;

-- =============================================
-- Mistral: Large 3 (December 2025)
-- =============================================
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
) VALUES (
    gen_random_uuid(), 'mistral-large-3', 'Mistral Large 3', 'mistral', 'chat',
    'https://api.mistral.ai', '', 'mistral-large-3-25-12',
    false, false, 19, 60, 3, '{}', '{}', NOW(), NOW()
) ON CONFLICT (name) DO NOTHING;
