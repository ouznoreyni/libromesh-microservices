#!/bin/bash

# setups/vault/init-secrets.sh
# Script to initialize Vault secrets for LibroMesh services

set -e

echo "Initializing Vault secrets..."

# Set Vault address and token
export VAULT_ADDR=${VAULT_ADDR:-http://localhost:8200}
export VAULT_TOKEN=${VAULT_TOKEN:-root-token}

# Wait for Vault to be fully ready
echo "Waiting for Vault to be ready..."
until vault status > /dev/null 2>&1; do
    echo "Vault not ready yet, waiting..."
    sleep 2
done

echo "Vault is ready, enabling KV secrets engine..."

# Enable KV secrets engine (v2) if not already enabled
vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV engine already enabled"

echo "Creating secrets for LibroMesh services..."

# Create secret for user-service
echo "Creating user-service secrets..."
vault kv put secret/user-service \
    keycloak.client-secret="libromesh-secret-2024" \
    keycloak.username="service-account-libromesh" \
    keycloak.server-url="http://localhost:8180" \
    keycloak.realm="libromesh" \
    keycloak.client-id="libroMesh"

# Create secret for book-service
echo "Creating book-service secrets..."
vault kv put secret/book-service \
    datasource.r2dbc_url="r2dbc:postgresql://localhost:5432/bookdb" \
    datasource.jdbc_url="jdbc:postgresql://localhost:5432/bookdb" \
    datasource.username="bookuser" \
    datasource.password="bookpass2025"

# Create secret for config-service
echo "Creating config-service secrets..."
vault kv put secret/config-service \
    vault.config.service-token="config-service-token-2025" \
    git.repository.url="https://github.com/libromesh/config-repo.git" \
    git.repository.username="config-user" \
    git.repository.password="config-pass-2025" \
    git.branch="main" \
    config.refresh.interval="30"

echo "Verifying secrets creation..."

# Verify secrets were created successfully
echo "Verifying user-service secrets..."
vault kv get secret/user-service

echo "Verifying book-service secrets..."
vault kv get secret/book-service

echo "Verifying config-service secrets..."
vault kv get secret/config-service

echo "All secrets have been successfully initialized in Vault!"

# Optional: Create policies for services
echo "Creating policies for services..."

# User service policy
vault policy write user-service-policy - <<EOF
path "secret/data/user-service" {
  capabilities = ["read"]
}
EOF

# Book service policy
vault policy write book-service-policy - <<EOF
path "secret/data/book-service" {
  capabilities = ["read"]
}
EOF

# Config service policy
vault policy write config-service-policy - <<EOF
path "secret/data/config-service" {
  capabilities = ["read"]
}
EOF

echo "Service policies created successfully!"
echo "Vault initialization completed!"