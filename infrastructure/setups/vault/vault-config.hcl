# HashiCorp Vault 1.19 Production Configuration
# Place this in infrastructure/vault/vault-config.hcl

# Vault storage backend
storage "postgresql" {
  connection_url = "postgres://vault_user:vault_password@postgres-vault:5432/vault?sslmode=disable"
  table          = "vault_kv_store"
  max_parallel   = "128"
}

# Alternative: File storage for development
# storage "file" {
#   path = "/vault/data"
# }

# Vault listener configuration
listener "tcp" {
  address         = "0.0.0.0:8200"
  cluster_address = "0.0.0.0:8201"
  tls_disable     = "true"  # Set to false in production with proper certs

  # For production, enable TLS:
  # tls_cert_file = "/vault/tls/vault.crt"
  # tls_key_file  = "/vault/tls/vault.key"
}

# Cluster configuration for HA setup
cluster_addr  = "http://0.0.0.0:8201"
api_addr      = "http://0.0.0.0:8200"

# Vault configuration
ui            = true
max_lease_ttl = "87600h"  # 10 years
default_lease_ttl = "87600h"

# Telemetry (optional)
telemetry {
  prometheus_retention_time = "30s"
  disable_hostname = true
  enable_hostname_label = false
}

# Plugin directory
plugin_directory = "/vault/plugins"

# Log level
log_level = "INFO"

# Disable mlock in development (enable in production)
disable_mlock = true

# Entropy configuration for better randomness
entropy "seal" {
  mode = "augmentation"
}