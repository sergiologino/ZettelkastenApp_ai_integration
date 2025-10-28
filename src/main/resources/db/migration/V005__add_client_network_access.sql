-- Migration: Add client_network_access table
-- Creates many-to-many relationship between clients and neural networks with access limits

-- Create the client_network_access table (only if it doesn't exist)
CREATE TABLE IF NOT EXISTS client_network_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_application_id UUID NOT NULL,
    neural_network_id UUID NOT NULL,
    daily_request_limit INTEGER,
    monthly_request_limit INTEGER,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_client_app FOREIGN KEY (client_application_id) REFERENCES client_applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_neural_network FOREIGN KEY (neural_network_id) REFERENCES neural_networks(id) ON DELETE CASCADE,
    CONSTRAINT uk_client_network UNIQUE (client_application_id, neural_network_id)
);

-- Create indexes for better performance (only if they don't exist)
CREATE INDEX IF NOT EXISTS idx_client_network_access_client_id ON client_network_access (client_application_id);
CREATE INDEX IF NOT EXISTS idx_client_network_access_network_id ON client_network_access (neural_network_id);

-- Create trigger for automatic updated_at update (only if it doesn't exist)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Drop trigger if exists and recreate it
DROP TRIGGER IF EXISTS update_client_network_access_updated_at ON client_network_access;
CREATE TRIGGER update_client_network_access_updated_at
BEFORE UPDATE ON client_network_access
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Seed initial access for existing clients to all active networks
-- Give all existing clients access to all active networks by default
INSERT INTO client_network_access (client_application_id, neural_network_id, daily_request_limit, monthly_request_limit)
SELECT ca.id, nn.id, NULL, NULL -- NULL means no specific limits, use global limits
FROM client_applications ca, neural_networks nn
WHERE nn.is_active = TRUE
ON CONFLICT (client_application_id, neural_network_id) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE client_network_access IS 'Many-to-many relationship between clients and neural networks with access limits';
COMMENT ON COLUMN client_network_access.daily_request_limit IS 'Daily request limit for this client-network pair (NULL = unlimited)';
COMMENT ON COLUMN client_network_access.monthly_request_limit IS 'Monthly request limit for this client-network pair (NULL = unlimited)';
