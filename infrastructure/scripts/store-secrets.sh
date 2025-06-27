#!/bin/bash
# vault-init-secrets-api.sh - Enhanced version with keystore and truststore support

# Configuration
VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="root-token"
KEYSTORE_BASE_DIR="./keystores"
SECRETS_PATH="secrets"  # Changed to match your requirement: secrets/certificate/{serviceName}

echo "Starting Vault secrets initialization using API..."
echo "Vault Address: $VAULT_ADDR"
echo "Using secrets path: $SECRETS_PATH"

# Function to make Vault API calls
vault_api() {
    local method="$1"
    local path="$2"
    local data="$3"

    local url="$VAULT_ADDR/v1/$path"
    local curl_cmd="curl -s -X $method -H 'X-Vault-Token: $VAULT_TOKEN' -H 'Content-Type: application/json'"

    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    curl_cmd="$curl_cmd '$url'"
    eval $curl_cmd
}

# Test connection
echo "Testing Vault connection..."
health_response=$(curl -s "$VAULT_ADDR/v1/sys/health")
if echo "$health_response" | grep -q '"sealed":false'; then
    echo "✓ Connected to Vault successfully"
else
    echo "✗ Failed to connect to Vault"
    exit 1
fi

# Check/Create secret engine
echo "Checking current secret engines..."
mounts_response=$(curl -s -H "X-Vault-Token: $VAULT_TOKEN" "$VAULT_ADDR/v1/sys/mounts")

if echo "$mounts_response" | grep -q "\"$SECRETS_PATH/\""; then
    echo "✓ Secret engine found at $SECRETS_PATH/"
else
    echo "Creating new kv-v2 secret engine at $SECRETS_PATH/..."
    create_response=$(curl -s -X POST \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"type":"kv-v2","description":"Certificates and keystores storage"}' \
        "$VAULT_ADDR/v1/sys/mounts/$SECRETS_PATH")

    if [ $? -eq 0 ] && ! echo "$create_response" | grep -q '"errors"'; then
        echo "✓ Created $SECRETS_PATH/ secret engine"
    else
        echo "✗ Failed to create secret engine"
        echo "Response: $create_response"
        exit 1
    fi
fi

# Discover actual keystore files
echo -e "\nDiscovering certificate files..."
echo "Base directory: $KEYSTORE_BASE_DIR"

if [ ! -d "$KEYSTORE_BASE_DIR" ]; then
    echo "Certificate directory not found. Searching for certificate files..."
    find . -name "*.p12" -o -name "*keystore*" -o -name "*truststore*" -o -name "*.pem" 2>/dev/null | head -20
    echo "Please update KEYSTORE_BASE_DIR in the script or create the expected directory structure."
else
    echo "Found certificate directory. Contents:"
    find "$KEYSTORE_BASE_DIR" -type f \( -name "*.p12" -o -name "*.pem" -o -name "*keystore*" -o -name "*truststore*" \) 2>/dev/null | while read file; do
        size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null)
        echo "  $file ($size bytes)"
    done
fi

# Enhanced function to store both keystore and truststore
store_certificates_api() {
    local service="$1"
    local keystore_file="$2"
    local keystore_password="$3"
    local truststore_file="$4"
    local truststore_password="$5"

    echo -e "\nProcessing $service certificates..."

    # Initialize variables for the combined certificate data
    local keystore_data=""
    local truststore_data=""
    local has_keystore=false
    local has_truststore=false

    # Process keystore
    if [ -n "$keystore_file" ] && [ -f "$keystore_file" ]; then
        echo "  Processing keystore: $keystore_file"

        file_size=$(stat -f%z "$keystore_file" 2>/dev/null || stat -c%s "$keystore_file" 2>/dev/null)
        echo "    File size: $file_size bytes"

        # Base64 encode keystore
        if command -v base64 >/dev/null 2>&1; then
            if ! keystore_data=$(base64 -w 0 "$keystore_file" 2>/dev/null); then
                if ! keystore_data=$(base64 -i "$keystore_file" 2>/dev/null); then
                    if ! keystore_data=$(base64 < "$keystore_file" 2>/dev/null); then
                        echo "    ✗ Failed to base64 encode keystore file"
                        return 1
                    fi
                fi
            fi
            keystore_data=$(echo "$keystore_data" | tr -d '\n\r')
            has_keystore=true
            echo "    ✓ Keystore encoded (${#keystore_data} characters)"
        else
            echo "    ✗ base64 command not found"
            return 1
        fi
    else
        echo "  Warning: Keystore file not found or not specified: $keystore_file"
    fi

    # Process truststore
    if [ -n "$truststore_file" ] && [ -f "$truststore_file" ]; then
        echo "  Processing truststore: $truststore_file"

        file_size=$(stat -f%z "$truststore_file" 2>/dev/null || stat -c%s "$truststore_file" 2>/dev/null)
        echo "    File size: $file_size bytes"

        # Base64 encode truststore
        if command -v base64 >/dev/null 2>&1; then
            if ! truststore_data=$(base64 -w 0 "$truststore_file" 2>/dev/null); then
                if ! truststore_data=$(base64 -i "$truststore_file" 2>/dev/null); then
                    if ! truststore_data=$(base64 < "$truststore_file" 2>/dev/null); then
                        echo "    ✗ Failed to base64 encode truststore file"
                        return 1
                    fi
                fi
            fi
            truststore_data=$(echo "$truststore_data" | tr -d '\n\r')
            has_truststore=true
            echo "    ✓ Truststore encoded (${#truststore_data} characters)"
        else
            echo "    ✗ base64 command not found"
            return 1
        fi
    else
        echo "  Warning: Truststore file not found or not specified: $truststore_file"
    fi

    # Check if we have at least one certificate file
    if [ "$has_keystore" = false ] && [ "$has_truststore" = false ]; then
        echo "  ✗ No valid certificate files found for $service"
        return 1
    fi

    # Create JSON data with all certificate information
    local json_data
    if command -v jq >/dev/null 2>&1; then
        # Use jq for proper JSON construction
        json_data=$(jq -n \
            --arg service "$service" \
            --arg keystore "$keystore_data" \
            --arg keystore_password "$keystore_password" \
            --arg keystore_type "PKCS12" \
            --arg truststore "$truststore_data" \
            --arg truststore_password "$truststore_password" \
            --arg truststore_type "PKCS12" \
            '{
                data: {
                    service: $service,
                    "key-store": $keystore,
                    "key-store-password": $keystore_password,
                    "key-store-type": $keystore_type,
                    "trust-store": $truststore,
                    "trust-store-password": $truststore_password,
                    "trust-store-type": $truststore_type
                }
            }')
    else
        # Fallback without jq - manual JSON construction
        escaped_keystore=$(echo "$keystore_data" | sed 's/"/\\"/g')
        escaped_truststore=$(echo "$truststore_data" | sed 's/"/\\"/g')

        json_data=$(cat <<EOF
{
  "data": {
    "service": "$service",
    "key-store": "$escaped_keystore",
    "key-store-password": "$keystore_password",
    "key-store-type": "PKCS12",
    "trust-store": "$escaped_truststore",
    "trust-store-password": "$truststore_password",
    "trust-store-type": "PKCS12"
  }
}
EOF
)
    fi

    # Store in Vault using the new path structure: secrets/certificate/{serviceName}
    vault_path="$SECRETS_PATH/data/certificate/$service"
    echo "  Storing at: $vault_path"

    response=$(curl -s -X POST \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$json_data" \
        "$VAULT_ADDR/v1/$vault_path")

    if [ $? -eq 0 ] && ! echo "$response" | grep -q '"errors"'; then
        echo "  ✓ Success - $service certificates stored"
        return 0
    else
        echo "  ✗ Failed to store $service certificates in Vault"
        echo "  Response: $response"
        return 1
    fi
}

# Store all service certificates
echo -e "\n=== Storing Service Certificates ==="

# Gateway Service
store_certificates_api "gateway-service" \
    "$KEYSTORE_BASE_DIR/gateway/gateway-service-keystore.p12" \
    "gateway-service-libromesh2024" \
    "$KEYSTORE_BASE_DIR/gateway/gateway-service-truststore.p12" \
    "libromesh-trust-2024"

# Book Service
store_certificates_api "book-service" \
    "$KEYSTORE_BASE_DIR/microservices/book-service-keystore.p12" \
    "book-service-libromesh2024" \
    "$KEYSTORE_BASE_DIR/microservices/book-service-truststore.p12" \
    "libromesh-trust-2024"

# User Service
store_certificates_api "user-service" \
    "$KEYSTORE_BASE_DIR/microservices/user-service-keystore.p12" \
    "user-service-libromesh2024" \
    "$KEYSTORE_BASE_DIR/microservices/user-service-truststore.p12" \
    "libromesh-trust-2024"

# Config Service
store_certificates_api "config-service" \
    "$KEYSTORE_BASE_DIR/infrastructure/config-service-keystore.p12" \
    "config-service-libromesh2024" \
    "$KEYSTORE_BASE_DIR/infrastructure/config-service-truststore.p12" \
    "libromesh-trust-2024"

# Discovery Service
store_certificates_api "discovery-service" \
    "$KEYSTORE_BASE_DIR/infrastructure/discovery-service-keystore.p12" \
    "discovery-service-libromesh2024" \
    "$KEYSTORE_BASE_DIR/infrastructure/discovery-service-truststore.p12" \
    "libromesh-trust-2024"

# Store CA certificate separately
echo -e "\n=== Processing CA Certificate ==="
ca_file="$KEYSTORE_BASE_DIR/service-ca-certificate.pem"
echo "Looking for CA certificate: $ca_file"

if [ -f "$ca_file" ]; then
    file_size=$(stat -f%z "$ca_file" 2>/dev/null || stat -c%s "$ca_file" 2>/dev/null)
    echo "CA file size: $file_size bytes"

    if ! encoded_ca=$(base64 -w 0 "$ca_file" 2>/dev/null); then
        if ! encoded_ca=$(base64 -i "$ca_file" 2>/dev/null); then
            if ! encoded_ca=$(base64 < "$ca_file" 2>/dev/null); then
                echo "✗ Failed to encode CA certificate"
            else
                encoded_ca=$(echo "$encoded_ca" | tr -d '\n\r')
            fi
        else
            encoded_ca=$(echo "$encoded_ca" | tr -d '\n\r')
        fi
    else
        encoded_ca=$(echo "$encoded_ca" | tr -d '\n\r')
    fi

    if [ -n "$encoded_ca" ]; then
        echo "CA encoded length: ${#encoded_ca} characters"

        # Create JSON with proper escaping
        if command -v jq >/dev/null 2>&1; then
            ca_json_data=$(jq -n \
                --arg cert "$encoded_ca" \
                --arg type "PEM" \
                '{
                    data: {
                        certificate: $cert,
                        type: $type,
                        description: "Service CA Certificate"
                    }
                }')
        else
            escaped_ca=$(echo "$encoded_ca" | sed 's/"/\\"/g')
            ca_json_data=$(cat <<EOF
{
  "data": {
    "certificate": "$escaped_ca",
    "type": "PEM",
    "description": "Service CA Certificate"
  }
}
EOF
)
        fi

        # Store CA at secrets/certificate/ca
        ca_response=$(curl -s -X POST \
            -H "X-Vault-Token: $VAULT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$ca_json_data" \
            "$VAULT_ADDR/v1/$SECRETS_PATH/data/certificate/ca")

        if [ $? -eq 0 ] && ! echo "$ca_response" | grep -q '"errors"'; then
            echo "✓ CA certificate stored at secrets/certificate/ca"
        else
            echo "✗ Failed to store CA certificate"
            echo "Response: $ca_response"
        fi
    fi
else
    echo "Warning: CA certificate not found: $ca_file"
    echo "Checking for alternative locations..."
    find . -name "*.pem" -type f 2>/dev/null | head -5
fi

# Create enhanced policies
echo -e "\n=== Creating Access Policies ==="

create_service_policy() {
    local service="$1"
    local policy_name="${service}-policy"

    local policy_content="# Policy for $service
path \"$SECRETS_PATH/data/certificate/$service\" {
  capabilities = [\"read\"]
}

path \"$SECRETS_PATH/metadata/certificate/$service\" {
  capabilities = [\"read\"]
}

# Allow access to CA certificate
path \"$SECRETS_PATH/data/certificate/ca\" {
  capabilities = [\"read\"]
}

path \"$SECRETS_PATH/metadata/certificate/ca\" {
  capabilities = [\"read\"]
}"

    local policy_json
    if command -v jq >/dev/null 2>&1; then
        policy_json=$(jq -n --arg policy "$policy_content" '{policy: $policy}')
    else
        local escaped_policy=$(printf '%s' "$policy_content" | sed ':a;N;$!ba;s/\n/\\n/g' | sed 's/"/\\"/g')
        policy_json=$(printf '{"policy": "%s"}' "$escaped_policy")
    fi

    response=$(curl -s -X PUT \
        -H "X-Vault-Token: $VAULT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$policy_json" \
        "$VAULT_ADDR/v1/sys/policies/acl/$policy_name")

    if [ $? -eq 0 ] && ! echo "$response" | grep -q '"errors"'; then
        echo "✓ Created policy: $policy_name"
    else
        echo "✗ Failed to create policy: $policy_name"
        echo "Response: $response"
    fi
}

# Create policies for all services
create_service_policy "gateway-service"
create_service_policy "book-service"
create_service_policy "user-service"
create_service_policy "config-service"
create_service_policy "discovery-service"

# Create a general CA policy
ca_only_policy_content="# CA Certificate access policy
path \"$SECRETS_PATH/data/certificate/ca\" {
  capabilities = [\"read\"]
}

path \"$SECRETS_PATH/metadata/certificate/ca\" {
  capabilities = [\"read\"]
}"

if command -v jq >/dev/null 2>&1; then
    ca_policy_json=$(jq -n --arg policy "$ca_only_policy_content" '{policy: $policy}')
else
    escaped_ca_policy=$(printf '%s' "$ca_only_policy_content" | sed ':a;N;$!ba;s/\n/\\n/g' | sed 's/"/\\"/g')
    ca_policy_json=$(printf '{"policy": "%s"}' "$escaped_ca_policy")
fi

response=$(curl -s -X PUT \
    -H "X-Vault-Token: $VAULT_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$ca_policy_json" \
    "$VAULT_ADDR/v1/sys/policies/acl/ca-certificate-policy")

if [ $? -eq 0 ] && ! echo "$response" | grep -q '"errors"'; then
    echo "✓ Created policy: ca-certificate-policy"
else
    echo "✗ Failed to create CA certificate policy"
fi

echo -e "\n=== Summary ==="
echo "Certificate storage structure:"
echo "  $SECRETS_PATH/certificate/gateway-service"
echo "  $SECRETS_PATH/certificate/book-service"
echo "  $SECRETS_PATH/certificate/user-service"
echo "  $SECRETS_PATH/certificate/config-service"
echo "  $SECRETS_PATH/certificate/discovery-service"
echo "  $SECRETS_PATH/certificate/ca"

echo -e "\nEach service certificate contains:"
echo "  - key-store (base64 encoded PKCS12)"
echo "  - key-store-password"
echo "  - key-store-type: PKCS12"
echo "  - trust-store (base64 encoded PKCS12)"
echo "  - trust-store-password"
echo "  - trust-store-type: PKCS12"

echo -e "\nTo verify certificates using API:"
echo "# List all certificates"
echo "curl -H 'X-Vault-Token: $VAULT_TOKEN' '$VAULT_ADDR/v1/$SECRETS_PATH/metadata/certificate?list=true'"

echo -e "\n# Get gateway service certificates"
echo "curl -H 'X-Vault-Token: $VAULT_TOKEN' '$VAULT_ADDR/v1/$SECRETS_PATH/data/certificate/gateway-service'"

echo -e "\n# Get CA certificate"
echo "curl -H 'X-Vault-Token: $VAULT_TOKEN' '$VAULT_ADDR/v1/$SECRETS_PATH/data/certificate/ca'"

echo -e "\n✓ Enhanced Vault initialization completed successfully!"
echo "✓ All certificates stored with keystore and truststore support"
echo "✓ Policies created for secure access control"