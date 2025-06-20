#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
VAULT_ADDR=${VAULT_ADDR:-'http://localhost:8200'}
VAULT_TOKEN=${VAULT_TOKEN:-'myroot'}
VAULT_NAMESPACE=${VAULT_NAMESPACE:-''}

echo -e "${BLUE}🏗️  Initializing HashiCorp Vault 1.19 for Library Management System${NC}"
echo "Vault Address: $VAULT_ADDR"

# Wait for Vault to be ready
echo -e "${YELLOW}⏳ Waiting for Vault to be ready...${NC}"
until vault status > /dev/null 2>&1; do
    sleep 2
done
echo -e "${GREEN}✅ Vault is ready${NC}"

# Set Vault address and token
export VAULT_ADDR=$VAULT_ADDR
export VAULT_TOKEN=$VAULT_TOKEN

# Enable KV v2 secrets engine with enhanced options
echo -e "${BLUE}🔐 Enabling KV v2 secrets engine...${NC}"
vault secrets enable -version=2 -path=secret kv || echo "KV engine already enabled"

# Enable KV v1 for legacy compatibility if needed
vault secrets enable -version=1 -path=legacy kv || echo "Legacy KV engine already enabled"

# Enable AppRole authentication
echo -e "${BLUE}🔑 Enabling AppRole authentication...${NC}"
vault auth enable approle || echo "AppRole already enabled"

# Enable additional auth methods for production
echo -e "${BLUE}🔐 Enabling additional auth methods...${NC}"
vault auth enable -path=kubernetes kubernetes || echo "Kubernetes auth already enabled"
vault auth enable -path=jwt jwt || echo "JWT auth already enabled"

# Enable audit logging
echo -e "${BLUE}📝 Enabling audit logging...${NC}"
vault audit enable file file_path=/vault/logs/audit.log || echo "File audit already enabled"

# Create policies for each service
echo -e "${BLUE}📋 Creating service policies...${NC}"

# Config Server Policy
vault policy write config-server-policy - <<EOF
# Config server needs access to all service secrets
path "secret/data/*" {
  capabilities = ["read"]
}
path "secret/metadata/*" {
  capabilities = ["list", "read"]
}
EOF

# Discovery Service Policy
vault policy write discovery-service-policy - <<EOF
path "secret/data/discovery-service/*" {
  capabilities = ["read"]
}
path "secret/data/application/*" {
  capabilities = ["read"]
}
EOF

# API Gateway Policy
vault policy write api-gateway-policy - <<EOF
path "secret/data/api-gateway/*" {
  capabilities = ["read"]
}
path "secret/data/application/*" {
  capabilities = ["read"]
}
EOF

# User Service Policy
vault policy write user-service-policy - <<EOF
path "secret/data/user-service/*" {
  capabilities = ["read"]
}
path "secret/data/application/*" {
  capabilities = ["read"]
}
EOF

# Book Service Policy
vault policy write book-service-policy - <<EOF
path "secret/data/book-service/*" {
  capabilities = ["read"]
}
path "secret/data/application/*" {
  capabilities = ["read"]
}
EOF

# Borrowing Service Policy
vault policy write borrowing-service-policy - <<EOF
path "secret/data/borrowing-service/*" {
  capabilities = ["read"]
}
path "secret/data/application/*" {
  capabilities = ["read"]
}
EOF

# Inventory Service Policy
vault policy write inventory-service-policy - <<EOF
path "secret/data/inventory-service/*" {
  capabilities = ["read"]
}
path "secret/data/application/*" {
  capabilities = ["read"]
}
EOF

# Create AppRoles for each service
echo -e "${BLUE}🎭 Creating AppRoles for services...${NC}"

services=("config-server" "discovery-service" "api-gateway" "user-service" "book-service" "borrowing-service" "inventory-service")

for service in "${services[@]}"; do
    echo "Creating AppRole for $service..."

    vault write auth/approle/role/$service-role \
        token_policies="$service-policy" \
        token_ttl=1h \
        token_max_ttl=4h \
        bind_secret_id=true \
        secret_id_ttl=10m

    # Get role-id and secret-id
    ROLE_ID=$(vault read -field=role_id auth/approle/role/$service-role/role-id)
    SECRET_ID=$(vault write -field=secret_id -f auth/approle/role/$service-role/secret-id)

    echo "Service: $service"
    echo "Role ID: $ROLE_ID"
    echo "Secret ID: $SECRET_ID"
    echo "---"
done

# Store global application secrets
echo -e "${BLUE}🌐 Storing global application secrets...${NC}"
vault kv put secret/application \
    jwt.secret="library-jwt-super-secret-key-2024" \
    encryption.key="library-encryption-key-12345" \
    database.admin.password="admin-super-secret-password" \
    monitoring.password="monitoring-secret-123"

# Store service-specific secrets
echo -e "${BLUE}🔒 Storing service-specific secrets...${NC}"

# Discovery Service Secrets
vault kv put secret/discovery-service \
    eureka.admin.password="eureka-admin-secret"

vault kv put secret/discovery-service/dev \
    eureka.admin.password="eureka-dev-admin"

vault kv put secret/discovery-service/prod \
    eureka.admin.password="eureka-prod-admin-secure"

# API Gateway Secrets
vault kv put secret/api-gateway \
    rate.limit.redis.password="redis-gateway-password"

vault kv put secret/api-gateway/dev \
    rate.limit.redis.password="redis-dev-password"

vault kv put secret/api-gateway/prod \
    rate.limit.redis.password="redis-prod-secure-password"

# User Service Secrets
vault kv put secret/user-service \
    database.password="user-db-password" \
    email.smtp.password="email-service-password" \
    oauth.google.client.secret="google-oauth-secret" \
    oauth.github.client.secret="github-oauth-secret"

vault kv put secret/user-service/dev \
    database.password="user-dev-db-password" \
    email.smtp.password="dev-email-password"

vault kv put secret/user-service/prod \
    database.password="user-prod-secure-db-password" \
    email.smtp.password="prod-email-secure-password"

# Book Service Secrets
vault kv put secret/book-service \
    database.password="book-db-password" \
    external.api.goodreads.key="goodreads-api-key" \
    external.api.openlibrary.key="openlibrary-api-key"

vault kv put secret/book-service/dev \
    database.password="book-dev-db-password"

vault kv put secret/book-service/prod \
    database.password="book-prod-secure-db-password"

# Borrowing Service Secrets
vault kv put secret/borrowing-service \
    database.password="borrowing-db-password" \
    notification.sms.api.key="sms-service-api-key" \
    payment.stripe.secret="stripe-secret-key"

vault kv put secret/borrowing-service/dev \
    database.password="borrowing-dev-db-password" \
    payment.stripe.secret="stripe-test-secret"

vault kv put secret/borrowing-service/prod \
    database.password="borrowing-prod-secure-db-password" \
    payment.stripe.secret="stripe-live-secret-key"

# Inventory Service Secrets
vault kv put secret/inventory-service \
    database.password="inventory-db-password" \
    barcode.scanner.api.key="barcode-api-key"

vault kv put secret/inventory-service/dev \
    database.password="inventory-dev-db-password"

vault kv put secret/inventory-service/prod \
    database.password="inventory-prod-secure-db-password"

echo -e "${GREEN}✅ Vault initialization completed successfully!${NC}"
echo -e "${YELLOW}📝 Service tokens and credentials have been created${NC}"
echo -e "${BLUE}🔍 You can view secrets with: vault kv get secret/<service-name>${NC}"

# Create a summary file
cat > vault-summary.txt <<EOF
Vault Initialization Summary
============================

Vault Address: $VAULT_ADDR
Root Token: $VAULT_TOKEN

Policies Created:
- config-server-policy
- discovery-service-policy
- api-gateway-policy
- user-service-policy
- book-service-policy
- borrowing-service-policy
- inventory-service-policy

AppRoles Created:
- config-server-role
- discovery-service-role
- api-gateway-role
- user-service-role
- book-service-role
- borrowing-service-role
- inventory-service-role

Secrets Stored:
- secret/application (global secrets)
- secret/<service-name> (service-specific secrets)
- secret/<service-name>/dev (development environment)
- secret/<service-name>/prod (production environment)

To get role credentials for a service:
vault read auth/approle/role/<service-name>-role/role-id
vault write -f auth/approle/role/<service-name>-role/secret-id

To view secrets:
vault kv get secret/<service-name>
EOF

echo -e "${GREEN}📄 Summary saved to vault-summary.txt${NC}"