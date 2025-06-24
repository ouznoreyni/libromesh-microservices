#!/bin/bash

# LibroMesh Vault Data Setup Script
# Exit on any error
set -e

# Vault address (use localhost:8200 from the host perspective)
export VAULT_ADDR=http://localhost:8200

# Container name from Docker Compose
CONTAINER_NAME="libromesh-vault"

# Color codes for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting LibroMesh Vault Setup${NC}"
echo "================================="

# Check if the container is running
if ! docker ps -q -f name=$CONTAINER_NAME > /dev/null; then
  echo -e "${RED}❌ Error: Container $CONTAINER_NAME is not running${NC}"
  echo "Please start Docker Compose with 'docker-compose up -d'"
  exit 1
fi

echo -e "${GREEN}✅ Container $CONTAINER_NAME is running${NC}"

# Wait for Vault to be ready (healthcheck ensures this)
echo -e "${YELLOW}⏳ Waiting for Vault to be ready...${NC}"
until docker exec $CONTAINER_NAME vault status &> /dev/null; do
  echo "Vault is not ready yet, waiting..."
  sleep 5
done
echo -e "${GREEN}✅ Vault is ready${NC}"

# Set the development root token from Docker Compose (VAULT_DEV_ROOT_TOKEN_ID)
export VAULT_TOKEN=root-token
echo -e "${BLUE}🔑 Using predefined root token: $VAULT_TOKEN${NC}"

# Verify the token is valid by attempting a lookup from inside the container
if ! docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault token lookup > /dev/null 2>&1; then
  echo -e "${RED}❌ Error: The VAULT_TOKEN '$VAULT_TOKEN' is invalid or lacks permissions${NC}"
  echo "Checking Vault logs for token details: docker logs $CONTAINER_NAME"
  echo "Please reinitialize Vault with 'docker-compose down -v' and 'docker-compose up -d' to reset the token, then re-run the script."
  exit 1
fi

echo -e "${GREEN}✅ Token validation successful${NC}"
echo ""

# Enable KV secrets engine (v2) if not already enabled
echo -e "${BLUE}📝 Enabling KV secrets engine...${NC}"
docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV secrets engine already enabled"

echo ""
echo -e "${BLUE}🔐 Creating Vault secrets...${NC}"
echo "============================"

# Common secrets for libromesh (default-context: libromesh/common)
echo "📝 Creating common application secrets..."
docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault kv put -format=json secret/libromesh/common \
  jwt.signing-key="your-secure-jwt-signing-key-2025" \
  jwt.issuer="https://libromesh.local" \
  jwt.audience="libromesh-api" \
  APP_JWT_TOKEN_VALIDITY="900" \
  APP_JWT_REFRESH_TOKEN_VALIDITY="604800000" \
  app.version="1.0.0-dev" \
  app.environment="development"

# Book-service secrets
echo "📝 Creating book-service secrets..."
docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault kv put -format=json secret/book-service \
  datasource.url="r2dbc:postgresql://localhost:5432/bookdb" \
  datasource.username="bookuser" \
  datasource.password="bookpass2025" \
  datasource.pool.max-size="20" \
  datasource.pool.initial-size="5" \
  VAULT_SSL_KEYSTORE_PASSWORD="secure-keystore-pass" \
  api.external.isbn.key="isbn-api-key-dev" \
  file.upload.path="/app/uploads/books" \
  file.upload.max-size="10MB"

# User-service secrets (Keycloak-related)
echo "📝 Creating user-service secrets..."
docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault kv put -format=json secret/user-service \
  LIBROMESH_CLIENT_SECRET="libromesh-secret-2025" \
  KEYCLOAK_SERVICE_USERNAME="service-account-libromesh" \
  KEYCLOAK_SERVICE_PASSWORD="service-pass-2025" \
  keycloak.server-url="http://localhost:8180" \
  keycloak.realm="libromesh" \
  keycloak.client-id="libromesh-app" \
  datasource.url="r2dbc:postgresql://localhost:5432/userdb" \
  datasource.username="userdbuser" \
  datasource.password="userdbpass2025"

# Config-service secrets
echo "📝 Creating config-service secrets..."
docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault kv put -format=json secret/config-service \
  VAULT_CONFIG_SERVICE_TOKEN="config-service-token-2025" \
  git.repository.url="https://github.com/libromesh/config-repo.git" \
  git.repository.username="config-user" \
  git.repository.password="config-pass-2025" \
  git.branch="main" \
  config.refresh.interval="30"

# Keycloak-related secrets (aligned with Docker Compose environment variables)
echo "📝 Creating Keycloak secrets..."
docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault kv put -format=json secret/keycloak \
  LIBROMESH_CLIENT_SECRET="libromesh-secret-2025" \
  LIBROMESH_URL="http://localhost:8080" \
  LIBROMESH_ADMIN_EMAIL="admin@libromesh.local" \
  LIBROMESH_ADMIN_USERNAME="admin" \
  LIBROMESH_ADMIN_PASSWORD="admin123" \
  LIBROMESH_LIBRARIAN_EMAIL="librarian@libromesh.local" \
  LIBROMESH_LIBRARIAN_USERNAME="librarian" \
  LIBROMESH_LIBRARIAN_PASSWORD="librarian123" \
  database.host="keycloak-db" \
  database.port="5432" \
  database.name="keycloak" \
  database.username="keycloak" \
  database.password="keycloak_password"

# Kafka-related secrets (optional, if needed for authentication)
echo "📝 Creating Kafka secrets..."
docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault kv put -format=json secret/kafka \
  KAFKA_BROKER_USERNAME="kafka-admin" \
  KAFKA_BROKER_PASSWORD="kafka-pass-2025" \
  bootstrap.servers="kafka:29092" \
  security.protocol="PLAINTEXT" \
  consumer.group.id="libromesh-consumer-group" \
  producer.acks="1" \
  producer.retries="3" \
  consumer.auto.offset.reset="earliest"

# Additional database secrets for other services
echo "📝 Creating database secrets..."
docker exec -e VAULT_TOKEN=$VAULT_TOKEN $CONTAINER_NAME vault kv put -format=json secret/database/keycloak \
  host="keycloak-db" \
  port="5432" \
  database="keycloak" \
  username="keycloak" \
  password="keycloak_password" \
  ssl-mode="disable" \
  max-connections="20"

echo ""
echo -e "${GREEN}✅ Vault secrets have been created successfully${NC}"
echo ""
echo -e "${BLUE}🔍 Verify with the following commands:${NC}"
echo "========================================="
echo "  docker exec $CONTAINER_NAME vault kv get secret/libromesh/common"
echo "  docker exec $CONTAINER_NAME vault kv get secret/book-service"
echo "  docker exec $CONTAINER_NAME vault kv get secret/user-service"
echo "  docker exec $CONTAINER_NAME vault kv get secret/config-service"
echo "  docker exec $CONTAINER_NAME vault kv get secret/keycloak"
echo "  docker exec $CONTAINER_NAME vault kv get secret/kafka"
echo "  docker exec $CONTAINER_NAME vault kv get secret/database/keycloak"
echo ""
echo -e "${GREEN}🎉 LibroMesh Vault Setup Complete!${NC}"