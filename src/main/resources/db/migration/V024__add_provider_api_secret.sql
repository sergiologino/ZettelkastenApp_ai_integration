ALTER TABLE neural_networks
    ADD COLUMN IF NOT EXISTS api_secret_encrypted TEXT;

COMMENT ON COLUMN neural_networks.api_secret_encrypted IS 'Encrypted secondary provider secret, for providers that require two credentials such as Kling Secret Key';
