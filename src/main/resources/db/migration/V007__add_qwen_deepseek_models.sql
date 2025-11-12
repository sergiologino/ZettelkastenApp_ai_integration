-- Add Qwen and DeepSeek neural networks
-- Migration V007: Add Chinese AI models support

-- ========================================
-- QWEN Models (Alibaba Cloud)
-- ========================================

-- Qwen-Turbo (Cheapest and fastest, ~$0.002/1K tokens)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'qwen-turbo',
    'Qwen-Turbo (Fast & Cheap)',
    'qwen',
    'chat',
    'https://dashscope.aliyuncs.com/api/v1',
    '', -- API key will be added by admin
    'qwen-turbo',
    false, -- Inactive until API key is set
    false,
    30,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Qwen-Plus (Balanced performance and cost, ~$0.008/1K tokens)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'qwen-plus',
    'Qwen-Plus (Balanced)',
    'qwen',
    'chat',
    'https://dashscope.aliyuncs.com/api/v1',
    '',
    'qwen-plus',
    false,
    false,
    25,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Qwen-Max (Most powerful, ~$0.02/1K tokens)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'qwen-max',
    'Qwen-Max (Most Powerful)',
    'qwen',
    'chat',
    'https://dashscope.aliyuncs.com/api/v1',
    '',
    'qwen-max',
    false,
    false,
    20,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Qwen2.5-72B-Instruct (Latest version, open weights, ~$0.009/1K tokens via API providers)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'qwen2.5-72b-instruct',
    'Qwen2.5-72B-Instruct (Latest)',
    'qwen',
    'chat',
    'https://dashscope.aliyuncs.com/api/v1',
    '',
    'qwen2.5-72b-instruct',
    false,
    false,
    18,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- ========================================
-- DeepSeek Models
-- ========================================

-- DeepSeek-Chat (General purpose, ~$0.0014/1M input tokens, $0.0028/1M output tokens)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'deepseek-chat',
    'DeepSeek-Chat (General)',
    'deepseek',
    'chat',
    'https://api.deepseek.com/v1',
    '', -- API key will be added by admin
    'deepseek-chat',
    false,
    false,
    28,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- DeepSeek-Coder (Specialized for code, ~$0.0014/1M input tokens, $0.0028/1M output tokens)
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'deepseek-coder',
    'DeepSeek-Coder (Code Specialist)',
    'deepseek',
    'chat',
    'https://api.deepseek.com/v1',
    '',
    'deepseek-coder',
    false,
    false,
    26,
    60,
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- DeepSeek-V3 (Latest flagship model, ~$0.0014/1M input tokens, $0.0028/1M output tokens)
-- Released December 2024, MoE architecture, 671B total parameters
INSERT INTO neural_networks (
    id, name, display_name, provider, network_type, api_url, api_key_encrypted,
    model_name, is_active, is_free, priority, timeout_seconds, max_retries,
    request_mapping, response_mapping, created_at, updated_at
)
VALUES (
    gen_random_uuid(),
    'deepseek-v3',
    'DeepSeek-V3 (Latest Flagship)',
    'deepseek',
    'chat',
    'https://api.deepseek.com/v1',
    '',
    'deepseek-chat', -- V3 is accessed via deepseek-chat endpoint
    false,
    false,
    16, -- Higher priority due to better performance
    90, -- Longer timeout for complex requests
    3,
    '{}',
    '{}',
    NOW(),
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Add default limits for Qwen networks
INSERT INTO network_limits (id, neural_network_id, user_type, limit_period, request_limit, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    nn.id,
    'free_user',
    'daily',
    30, -- Higher limits for cheaper models
    NOW(),
    NOW()
FROM neural_networks nn
WHERE nn.provider IN ('qwen', 'deepseek')
ON CONFLICT DO NOTHING;

-- Add higher limits for paid users on Qwen/DeepSeek
INSERT INTO network_limits (id, neural_network_id, user_type, limit_period, request_limit, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    nn.id,
    'paid_user',
    'monthly',
    500, -- Higher monthly limits due to low cost
    NOW(),
    NOW()
FROM neural_networks nn
WHERE nn.provider IN ('qwen', 'deepseek')
ON CONFLICT DO NOTHING;

-- Comments for reference:
-- Qwen models: https://help.aliyun.com/zh/dashscope/developer-reference/api-details
-- DeepSeek models: https://platform.deepseek.com/api-docs/
-- 
-- Pricing notes:
-- - Qwen-Turbo: ~$0.002 per 1K tokens (cheapest)
-- - Qwen-Plus: ~$0.008 per 1K tokens (balanced)
-- - Qwen-Max: ~$0.02 per 1K tokens (most powerful)
-- - Qwen2.5-72B: ~$0.009 per 1K tokens (latest open model)
-- - DeepSeek-Chat: ~$0.0014 per 1M input tokens (extremely cheap!)
-- - DeepSeek-Coder: ~$0.0014 per 1M input tokens (code specialist)
-- - DeepSeek-V3: ~$0.0014 per 1M input tokens (latest, December 2024)

