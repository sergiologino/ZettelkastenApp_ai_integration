-- User accounts for end-user (non-admin) authentication
CREATE TABLE IF NOT EXISTS user_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255),
    provider VARCHAR(50),           -- local | google | yandex
    provider_subject VARCHAR(255),  -- sub from provider
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Link between user and client applications they own
CREATE TABLE IF NOT EXISTS user_client_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    client_app_id UUID NOT NULL REFERENCES client_applications(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, client_app_id)
);

-- OAuth state storage for authorization code flow
CREATE TABLE IF NOT EXISTS oauth_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state VARCHAR(128) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL,
    redirect_uri TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    consumed BOOLEAN NOT NULL DEFAULT FALSE
);


