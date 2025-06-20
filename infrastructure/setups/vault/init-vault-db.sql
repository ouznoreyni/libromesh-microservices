-- HashiCorp Vault 1.19 PostgreSQL Backend Initialization
-- This script prepares the PostgreSQL database for Vault storage

-- Create the Vault table if it doesn't exist
CREATE TABLE IF NOT EXISTS vault_kv_store (
                                              parent_path TEXT COLLATE "C" NOT NULL,
                                              path        TEXT COLLATE "C",
                                              key         TEXT COLLATE "C",
                                              value       BYTEA,
                                              CONSTRAINT pkey PRIMARY KEY (path, key)
    );

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS parent_path_idx ON vault_kv_store (parent_path);
CREATE INDEX IF NOT EXISTS path_key_idx ON vault_kv_store (path, key);

-- Create additional tables for HA (High Availability) if needed
CREATE TABLE IF NOT EXISTS vault_ha_locks (
                                              ha_key      TEXT COLLATE "C" NOT NULL,
                                              ha_identity TEXT COLLATE "C" NOT NULL,
                                              ha_value    BYTEA,
                                              valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
                                              CONSTRAINT ha_key PRIMARY KEY (ha_key)
    );

-- Grant permissions to vault user
GRANT ALL PRIVILEGES ON vault_kv_store TO vault_user;
GRANT ALL PRIVILEGES ON vault_ha_locks TO vault_user;

-- Create sequences if needed
-- These might be used by Vault for certain operations
CREATE SEQUENCE IF NOT EXISTS vault_sequence;
GRANT USAGE, SELECT ON SEQUENCE vault_sequence TO vault_user;

-- Create a view for monitoring (optional)
CREATE OR REPLACE VIEW vault_storage_stats AS
SELECT
    COUNT(*) as total_keys,
    COUNT(DISTINCT parent_path) as unique_paths,
    pg_size_pretty(pg_total_relation_size('vault_kv_store')) as table_size
FROM vault_kv_store;

GRANT SELECT ON vault_storage_stats TO vault_user;

-- Log the initialization
INSERT INTO vault_kv_store (parent_path, path, key, value)
VALUES ('system/', 'system/initialized', 'timestamp', encode(extract(epoch from now())::text::bytea, 'base64'))
    ON CONFLICT (path, key) DO NOTHING;