-- Migration: Add deleted field to client_applications table
-- Allows soft deletion of clients while preserving historical data

-- Add deleted column
ALTER TABLE client_applications ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;

-- Add index for better performance when filtering non-deleted clients
CREATE INDEX IF NOT EXISTS idx_client_applications_deleted ON client_applications (deleted);

-- Add comment for documentation
COMMENT ON COLUMN client_applications.deleted IS 'Soft deletion flag - deleted clients are hidden but preserved for historical data';
