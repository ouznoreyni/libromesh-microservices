#!/bin/bash

# File: scripts/init-vault.sh
# Vault Initialization Script for LibroMesh - Keycloak Admin Client Setup

set -e

# Configuration
VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="root-token"
VAULT_NAMESPACE="libromesh"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Vault is running
check_vault_status() {
    log_info "Checking Vault status..."
    if ! curl -s "$VAULT_ADDR/v1/sys/health" > /dev/null; then
        log_error "Vault is not accessible at $VAULT_ADDR"
        log_error "Please ensure Vault is running and accessible"
        exit 1
    fi
    log_info "Vault is accessible"
}

# Enable KV secrets engine
enable_kv_engine() {
    log_info "Enabling KV v2 secrets engine..."
    curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
         -H "Content-Type: application/json" \
         -X POST \
         -d '{"type":"kv","options":{"version":"2"}}' \
         "$VAULT_ADDR/v1/sys/mounts/secret" || true
    log_info "KV v2 secrets engine enabled"
}

# Create Keycloak Admin Client configuration
create_keycloak_admin_config() {
    log_info "Creating Keycloak Admin Client configuration..."

    cat > /tmp/keycloak_admin_config.json << EOF
{
  "data": {
    "server-url": "http://keycloak:8180/auth",
    "realm": "libromesh",
    "client-id": "libromesh-admin-client",
    "client-secret": "your-secure-admin-client-secret",
    "grant-type": "client_credentials"
  }
}
EOF

    curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
         -H "Content-Type: application/json" \
         -X POST \
         -d @/tmp/keycloak_admin_config.json \
         "$VAULT_ADDR/v1/secret/data/libromesh/keycloak-admin"

    rm /tmp/keycloak_admin_config.json
    log_info "Keycloak Admin Client configuration created"
}

# Create user-service configuration with Keycloak Admin references
create_user_service_config() {
    log_info "Creating user-service configuration..."

    cat > /tmp/user_service_config.json << EOF
{
  "data": {
    "server": {
      "port": 8081
    },
    "spring": {
      "application": {
        "name": "user-service"
      },
      "datasource": {
        "url": "\${vault:secret/data/libromesh/database#data.url}",
        "username": "\${vault:secret/data/libromesh/database#data.username}",
        "password": "\${vault:secret/data/libromesh/database#data.password}"
      }
    },
    "keycloak": {
      "admin": {
        "server-url": "\${vault:secret/data/libromesh/keycloak-admin#data.server-url}",
        "realm": "\${vault:secret/data/libromesh/keycloak-admin#data.realm}",
        "client-id": "\${vault:secret/data/libromesh/keycloak-admin#data.client-id}",
        "client-secret": "\${vault:secret/data/libromesh/keycloak-admin#data.client-secret}",
        "grant-type": "\${vault:secret/data/libromesh/keycloak-admin#data.grant-type}"
      }
    },
    "management": {
      "endpoints": {
        "web": {
          "exposure": {
            "include": "health,info,metrics"
          }
        }
      }
    }
  }
}
EOF

    curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
         -H "Content-Type: application/json" \
         -X POST \
         -d @/tmp/user_service_config.json \
         "$VAULT_ADDR/v1/secret/data/libromesh/user-service"

    rm /tmp/user_service_config.json
    log_info "user-service configuration created"
}

# Verify configurations
verify_configurations() {
    log_info "Verifying configurations..."

    paths=("keycloak-admin" "user-service")

    for path in "${paths[@]}"; do
        response=$(curl -s -H "X-Vault-Token: $VAULT_TOKEN" "$VAULT_ADDR/v1/secret/data/libromesh/$path")

        if echo "$response" | grep -q "\"data\""; then
            log_info "✓ Configuration verified: $path"
        else
            log_error "✗ Configuration missing: $path"
        fi
    done
}

# Main execution
main() {
    log_info "Starting Vault initialization for Keycloak Admin Client..."

    check_vault_status
    enable_kv_engine
    create_keycloak_admin_config
    create_user_service_config
    verify_configurations

    log_info "Vault initialization completed successfully!"
    log_info "Keycloak Admin Client is ready for user-service"
}

# Run main function
main "$@"