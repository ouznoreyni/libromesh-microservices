#!/bin/bash

# LibroMesh Microservices - Gateway HTTPS + Microservice mTLS Generator
# Architecture: Gateway (HTTPS) -> Microservices (mTLS)
# Project: LibroMesh
# Developer: ouznoreyni
# Location: Senegal

set -e

# ================================
# CONFIGURATION VARIABLES
# ================================

# Project Information
PROJECT_NAME="LibroMesh"
DEVELOPER_NAME="ouznoreyni"
ORGANIZATION="LibroMesh Platform"
COUNTRY="SN"
STATE="Dakar"
LOCATION="Dakar"

# Certificate Configuration
VALIDITY_DAYS=365
KEY_SIZE=2048

# Gateway Service (HTTPS only - accepts external clients)
GATEWAY_SERVICE="gateway-service:8080"

# Microservices (mTLS required - only accept requests from gateway)
MICROSERVICES=(
    "book-service:9002"
    "user-service:9001"
)

# Infrastructure Services (HTTPS only)
INFRASTRUCTURE_SERVICES=(
    "config-service:8000"
    "discovery-service:8761"
)

# Passwords
KEYSTORE_SUFFIX="libromesh2024"
TRUSTSTORE_PASSWORD="libromesh-trust-2024"

# ================================
# COLORS
# ================================
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# ================================
# FUNCTIONS
# ================================

print_step() { echo -e "${BLUE}🔄 $1${NC}"; }
print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_header() { echo -e "${PURPLE}======= $1 =======${NC}"; }
print_info() { echo -e "${CYAN}ℹ️  $1${NC}"; }

# Check dependencies
check_dependencies() {
    print_step "Checking dependencies..."
    for dep in openssl keytool; do
        if ! command -v $dep &> /dev/null; then
            print_error "$dep not found. Please install OpenSSL and Java JDK"
            exit 1
        fi
    done
    print_success "Dependencies OK"
}

# Setup directories
setup_directories() {
    print_step "Setting up directories..."
    rm -rf keystores temp
    mkdir -p keystores/{gateway,microservices,infrastructure} temp/{ca,certs}
    print_success "Directories created"
}

# Generate CA for mTLS communication between gateway and microservices
generate_ca() {
    print_step "Generating Certificate Authority for service-to-service communication..."

    openssl genrsa -out temp/ca/ca-key.pem $KEY_SIZE

    openssl req -new -x509 -sha256 -days $VALIDITY_DAYS \
        -key temp/ca/ca-key.pem \
        -out temp/ca/ca-cert.pem \
        -subj "/C=$COUNTRY/ST=$STATE/L=$LOCATION/O=$ORGANIZATION/OU=Service-CA/CN=$PROJECT_NAME-Service-CA"

    print_success "CA generated for service-to-service mTLS"
}

# Generate gateway keystore (HTTPS only for external clients + mTLS client cert for microservices)
generate_gateway_keystore() {
    IFS=':' read -r service_name port <<< "$GATEWAY_SERVICE"
    local keystore_password="${service_name}-${KEYSTORE_SUFFIX}"

    print_step "Generating gateway keystore (HTTPS for clients + mTLS client for services)..."

    # Generate gateway private key
    openssl genrsa -out temp/certs/${service_name}-key.pem $KEY_SIZE

    # Generate gateway CSR
    openssl req -new -sha256 \
        -key temp/certs/${service_name}-key.pem \
        -out temp/certs/${service_name}.csr \
        -subj "/C=$COUNTRY/ST=$STATE/L=$LOCATION/O=$ORGANIZATION/OU=Gateway/CN=$service_name"

    # Create extensions for gateway (server auth for HTTPS, client auth for mTLS)
    cat > temp/certs/${service_name}.ext << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth, clientAuth
subjectAltName = DNS:localhost,DNS:$service_name,DNS:gateway,DNS:api-gateway,IP:127.0.0.1,IP:0.0.0.0
EOF

    # Sign gateway certificate with CA (for mTLS with microservices)
    openssl x509 -req -in temp/certs/${service_name}.csr \
        -CA temp/ca/ca-cert.pem \
        -CAkey temp/ca/ca-key.pem \
        -CAcreateserial \
        -out temp/certs/${service_name}-cert.pem \
        -days $VALIDITY_DAYS \
        -sha256 \
        -extfile temp/certs/${service_name}.ext

    # Create gateway keystore (contains both server cert for HTTPS and client cert for mTLS)
    openssl pkcs12 -export \
        -out keystores/gateway/${service_name}-keystore.p12 \
        -inkey temp/certs/${service_name}-key.pem \
        -in temp/certs/${service_name}-cert.pem \
        -certfile temp/ca/ca-cert.pem \
        -password pass:$keystore_password \
        -name $service_name

    # Create truststore for gateway (to trust microservice certificates)
    keytool -import -trustcacerts -noprompt \
        -alias service-ca \
        -file temp/ca/ca-cert.pem \
        -keystore keystores/gateway/${service_name}-truststore.p12 \
        -storetype PKCS12 \
        -storepass $TRUSTSTORE_PASSWORD

    # Create gateway configuration
    cat > keystores/gateway/${service_name}-config.yml << EOF
# $PROJECT_NAME - Gateway Configuration
# Accepts HTTPS from external clients
# Uses mTLS when calling microservices

server:
  port: $port
  ssl:
    enabled: true
    key-store: classpath:keystores/${service_name}-keystore.p12
    key-store-password: $keystore_password
    key-store-type: PKCS12
    # NO client-auth required for external clients
    # client-auth: none  (default)

spring:
  application:
    name: $service_name
  cloud:
    gateway:
      routes:
        - id: book-service
          uri: https://localhost:8081
          predicates:
            - Path=/api/books/**
          filters:
            - StripPrefix=2
        - id: user-service
          uri: https://localhost:8082
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=2
      httpclient:
        ssl:
          # Gateway acts as mTLS client when calling microservices
          use-insecure-trust-manager: false
          key-store: classpath:keystores/${service_name}-keystore.p12
          key-store-password: $keystore_password
          key-store-type: PKCS12
          trust-store: classpath:keystores/${service_name}-truststore.p12
          trust-store-password: $TRUSTSTORE_PASSWORD
          trust-store-type: PKCS12

# Security configuration
security:
  # External clients use standard authentication (JWT, OAuth2, etc.)
  jwt:
    enabled: true
  # No mTLS required for external clients

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
EOF

    print_success "Gateway keystore created (HTTPS for clients, mTLS client for services)"
}

# Generate microservice keystore (requires mTLS from gateway only)
generate_microservice_keystore() {
    local service_name=$1
    local port=$2
    local keystore_password="${service_name}-${KEYSTORE_SUFFIX}"

    print_step "Generating microservice keystore for $service_name (mTLS required)..."

    # Generate microservice private key
    openssl genrsa -out temp/certs/${service_name}-key.pem $KEY_SIZE

    # Generate microservice CSR
    openssl req -new -sha256 \
        -key temp/certs/${service_name}-key.pem \
        -out temp/certs/${service_name}.csr \
        -subj "/C=$COUNTRY/ST=$STATE/L=$LOCATION/O=$ORGANIZATION/OU=Microservices/CN=$service_name"

    # Create extensions for microservice (server auth only)
    cat > temp/certs/${service_name}.ext << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = DNS:localhost,DNS:$service_name,IP:127.0.0.1
EOF

    # Sign microservice certificate with CA
    openssl x509 -req -in temp/certs/${service_name}.csr \
        -CA temp/ca/ca-cert.pem \
        -CAkey temp/ca/ca-key.pem \
        -CAcreateserial \
        -out temp/certs/${service_name}-cert.pem \
        -days $VALIDITY_DAYS \
        -sha256 \
        -extfile temp/certs/${service_name}.ext

    # Create microservice keystore
    openssl pkcs12 -export \
        -out keystores/microservices/${service_name}-keystore.p12 \
        -inkey temp/certs/${service_name}-key.pem \
        -in temp/certs/${service_name}-cert.pem \
        -certfile temp/ca/ca-cert.pem \
        -password pass:$keystore_password \
        -name $service_name

    # Create truststore for microservice (to trust gateway certificate)
    keytool -import -trustcacerts -noprompt \
        -alias service-ca \
        -file temp/ca/ca-cert.pem \
        -keystore keystores/microservices/${service_name}-truststore.p12 \
        -storetype PKCS12 \
        -storepass $TRUSTSTORE_PASSWORD

    # Create microservice configuration
    cat > keystores/microservices/${service_name}-config.yml << EOF
# $PROJECT_NAME - $service_name Configuration
# Requires mTLS client certificates (only gateway allowed)

server:
  port: $port
  ssl:
    enabled: true
    key-store: classpath:keystores/${service_name}-keystore.p12
    key-store-password: $keystore_password
    key-store-type: PKCS12
    trust-store: classpath:keystores/${service_name}-truststore.p12
    trust-store-password: $TRUSTSTORE_PASSWORD
    trust-store-type: PKCS12
    client-auth: need  # Require client certificates

spring:
  application:
    name: $service_name

# Security configuration - only allow gateway
app:
  security:
    mtls:
      enabled: true
      allowed-clients:
        - "CN=gateway-service,OU=Gateway,O=$ORGANIZATION,C=$COUNTRY"
      reject-direct-access: true

# Service discovery (if using Eureka)
eureka:
  client:
    service-url:
      defaultZone: https://localhost:8761/eureka/
    tls:
      enabled: false  # Eureka doesn't use mTLS
  instance:
    secure-port: $port
    secure-port-enabled: true
    non-secure-port-enabled: false

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when_authorized
EOF

    print_success "Microservice keystore created for $service_name (mTLS required)"
}

# Generate infrastructure service keystore (HTTPS only)
generate_infrastructure_keystore() {
    local service_name=$1
    local port=$2
    local keystore_password="${service_name}-${KEYSTORE_SUFFIX}"

    print_step "Generating infrastructure keystore for $service_name (HTTPS only)..."

    # Generate private key
    openssl genrsa -out temp/certs/${service_name}-key.pem $KEY_SIZE

    # Create self-signed certificate (no CA needed)
    openssl req -new -x509 -sha256 -days $VALIDITY_DAYS \
        -key temp/certs/${service_name}-key.pem \
        -out temp/certs/${service_name}-cert.pem \
        -subj "/C=$COUNTRY/ST=$STATE/L=$LOCATION/O=$ORGANIZATION/OU=Infrastructure/CN=$service_name" \
        -extensions v3_req \
        -config <(cat <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
[req_distinguished_name]
[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = DNS:localhost,DNS:$service_name,IP:127.0.0.1
EOF
)

    # Create PKCS12 keystore
    openssl pkcs12 -export \
        -out keystores/infrastructure/${service_name}-keystore.p12 \
        -inkey temp/certs/${service_name}-key.pem \
        -in temp/certs/${service_name}-cert.pem \
        -password pass:$keystore_password \
        -name $service_name

    # Create infrastructure config
    cat > keystores/infrastructure/${service_name}-config.yml << EOF
# $PROJECT_NAME - $service_name Infrastructure Configuration
# HTTPS only, no client certificates required

server:
  port: $port
  ssl:
    enabled: true
    key-store: classpath:keystores/${service_name}-keystore.p12
    key-store-password: $keystore_password
    key-store-type: PKCS12
    # No client-auth required

spring:
  application:
    name: $service_name

# Basic authentication for infrastructure services
security:
  user:
    name: ${service_name}-user
    password: ${service_name}-pass-2024
EOF

    # Add service-specific config
    if [[ "$service_name" == "config-service" ]]; then
        cat >> keystores/infrastructure/${service_name}-config.yml << EOF

# Config Server specific settings
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/$DEVELOPER_NAME/libromesh-config
          default-label: main
EOF
    elif [[ "$service_name" == "discovery-service" ]]; then
        cat >> keystores/infrastructure/${service_name}-config.yml << EOF

# Eureka Server specific settings
eureka:
  instance:
    hostname: localhost
    secure-port: $port
    secure-port-enabled: true
    non-secure-port-enabled: false
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: https://localhost:$port/eureka/
EOF
    fi

    print_success "Infrastructure keystore created for $service_name"
}

# Generate test certificates
generate_test_clients() {
    print_step "Generating test clients..."

    # External client (no certificate needed)
    cat > keystores/test-external-client.txt << EOF
# Test External Client (Regular HTTPS to Gateway)
# No client certificate required

# Test gateway endpoints
curl -k https://localhost:8080/actuator/health
curl -k https://localhost:8080/api/books
curl -k https://localhost:8080/api/users

# With authentication (add your JWT/OAuth2 token)
curl -k -H "Authorization: Bearer YOUR_JWT_TOKEN" https://localhost:8080/api/books
EOF

    # Service test client (mTLS certificate for direct microservice testing)
    local client_name="service-test-client"
    local client_password="service-test-2024"

    openssl genrsa -out temp/certs/${client_name}-key.pem $KEY_SIZE

    openssl req -new -sha256 \
        -key temp/certs/${client_name}-key.pem \
        -out temp/certs/${client_name}.csr \
        -subj "/C=$COUNTRY/ST=$STATE/L=$LOCATION/O=$ORGANIZATION/OU=Test-Clients/CN=$client_name"

    cat > temp/certs/${client_name}.ext << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
extendedKeyUsage = clientAuth
EOF

    openssl x509 -req -in temp/certs/${client_name}.csr \
        -CA temp/ca/ca-cert.pem \
        -CAkey temp/ca/ca-key.pem \
        -CAcreateserial \
        -out temp/certs/${client_name}-cert.pem \
        -days $VALIDITY_DAYS \
        -sha256 \
        -extfile temp/certs/${client_name}.ext

    openssl pkcs12 -export \
        -out keystores/${client_name}-keystore.p12 \
        -inkey temp/certs/${client_name}-key.pem \
        -in temp/certs/${client_name}-cert.pem \
        -certfile temp/ca/ca-cert.pem \
        -password pass:$client_password \
        -name $client_name

    cat > keystores/test-service-client.txt << EOF
# Test Service Client (mTLS for direct microservice testing)
# Use this ONLY for testing - in production, only gateway should call microservices

# Test microservices directly (requires mTLS)
curl -k --cert-type P12 --cert keystores/${client_name}-keystore.p12:$client_password
https://localhost:9002/actuator/health
curl -k --cert-type P12 --cert keystores/${client_name}-keystore.p12:$client_password
https://localhost:9001/actuator/health

# Test infrastructure services (HTTPS only)
curl -k https://localhost:8000/actuator/health
curl -k https://localhost:8761/actuator/health
EOF

    print_success "Test clients generated"
}

# Create comprehensive documentation
create_documentation() {
    print_step "Creating documentation..."

    cat > keystores/README.md << EOF
# $PROJECT_NAME - Security Architecture

## Overview
This architecture implements a **Gateway HTTPS + Microservice mTLS** pattern:

- **External clients** → **Gateway** (HTTPS only, no client certs)
- **Gateway** → **Microservices** (mTLS required)
- **Infrastructure services** (HTTPS only)

## Architecture Diagram

\`\`\`
[External Client] --HTTPS--> [Gateway] --mTLS--> [Microservices]
                                |
                            [Infrastructure Services] (HTTPS)
\`\`\`

## Services Configuration

### Gateway Service (Port 8080)
- **External Interface**: HTTPS (no client certificates required)
- **Internal Interface**: mTLS client (to call microservices)
- **Authentication**: JWT/OAuth2 for external clients
- **Location**: \`keystores/gateway/\`

### Microservices
EOF

    for service_config in "${MICROSERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        cat >> keystores/README.md << EOF
- **$service_name** (Port $port): Requires mTLS client certificates (only gateway allowed)
EOF
    done

    cat >> keystores/README.md << EOF
- **Location**: \`keystores/microservices/\`

### Infrastructure Services
EOF

    for service_config in "${INFRASTRUCTURE_SERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        cat >> keystores/README.md << EOF
- **$service_name** (Port $port): HTTPS + Basic Auth
EOF
    done

    cat >> keystores/README.md << EOF
- **Location**: \`keystores/infrastructure/\`

## Security Benefits

### ✅ **For External Clients**
- Simple HTTPS connection to gateway
- No certificate management for clients
- Standard authentication (JWT, OAuth2, API keys)

### ✅ **For Internal Communication**
- Strong mTLS between gateway and microservices
- Microservices isolated from direct external access
- Certificate-based service identity

### ✅ **For Operations**
- Infrastructure services use simple HTTPS
- Reduced certificate complexity
- Clear security boundaries

## Setup Instructions

### 1. Gateway Service
\`\`\`bash
cp keystores/gateway/* gateway-service/src/main/resources/keystores/
\`\`\`

### 2. Microservices
\`\`\`bash
cp keystores/microservices/book-service-* book-service/src/main/resources/keystores/
cp keystores/microservices/user-service-* user-service/src/main/resources/keystores/
\`\`\`

### 3. Infrastructure Services
\`\`\`bash
cp keystores/infrastructure/config-service-* config-service/src/main/resources/keystores/
cp keystores/infrastructure/discovery-service-* discovery-service/src/main/resources/keystores/
\`\`\`

## Testing

### External Client Testing (No Certificates)
\`\`\`bash
# Test gateway (public endpoint)
curl -k https://localhost:8080/actuator/health
curl -k https://localhost:8080/api/books
\`\`\`

### Service Testing (mTLS Required)
\`\`\`bash
# Test microservices directly (admin/debug only)
curl -k --cert-type P12 --cert keystores/service-test-client-keystore.p12:service-test-2024 \\
     https://localhost:9002/actuator/health
\`\`\`

## Network Security

### Firewall Rules
\`\`\`bash
# Allow external access to gateway only
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT

# Block direct access to microservices from external
iptables -A INPUT -p tcp --dport 9002 -s <gateway-ip> -j ACCEPT
iptables -A INPUT -p tcp --dport 9002 -j DROP
iptables -A INPUT -p tcp --dport 9001 -s <gateway-ip> -j ACCEPT
iptables -A INPUT -p tcp --dport 9001 -j DROP

# Allow infrastructure services from internal network only
iptables -A INPUT -p tcp --dport 8000 -s <internal-network> -j ACCEPT
iptables -A INPUT -p tcp --dport 8761 -s <internal-network> -j ACCEPT
\`\`\`

## Certificate Management

### Rotation Strategy
1. **Microservices**: Rotate certificates every 90 days
2. **Gateway**: Coordinate rotation with microservices
3. **Infrastructure**: Annual rotation acceptable

### Monitoring
- Monitor certificate expiration
- Verify mTLS handshakes
- Track authentication failures

## Troubleshooting

### Common Issues
1. **Gateway can't reach microservices**: Check mTLS client configuration
2. **External clients can't reach gateway**: Verify HTTPS configuration
3. **Certificate errors**: Check CA certificate in truststores

### Debug Commands
\`\`\`bash
# Test SSL connection
openssl s_client -connect localhost:8081 -cert gateway-cert.pem -key gateway-key.pem

# Verify keystore
keytool -list -keystore service-keystore.p12 -storetype PKCS12
\`\`\`

---
**Developer**: $DEVELOPER_NAME
**Project**: $PROJECT_NAME
**Location**: $LOCATION, Senegal
EOF

    print_success "Documentation created"
}

# Main execution
main() {
    echo "=================================================="
    echo "  $PROJECT_NAME - Gateway HTTPS + mTLS Architecture"
    echo "=================================================="
    echo "Developer: $DEVELOPER_NAME"
    echo "Location: $LOCATION, $STATE, Senegal"
    echo "=================================================="
    echo

    print_header "Security Architecture"
    echo "🌐 Gateway Service:"
    IFS=':' read -r gateway_name gateway_port <<< "$GATEWAY_SERVICE"
    echo "   • $gateway_name (port $gateway_port) - HTTPS for clients, mTLS client for services"
    echo
    echo "🔒 Microservices (mTLS required):"
    for service_config in "${MICROSERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        echo "   • $service_name (port $port) - Only accepts requests from gateway"
    done
    echo
    echo "🏗️ Infrastructure Services (HTTPS only):"
    for service_config in "${INFRASTRUCTURE_SERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        echo "   • $service_name (port $port) - HTTPS + Basic Auth"
    done
    echo

    read -p "Continue with this architecture? (Y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Nn]$ ]]; then
        echo "Cancelled"
        exit 0
    fi

    # Execute steps
    check_dependencies
    setup_directories
    generate_ca

    # Generate certificates
    print_header "Generating Gateway Certificates"
    generate_gateway_keystore

    print_header "Generating Microservice Certificates"
    for service_config in "${MICROSERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        generate_microservice_keystore "$service_name" "$port"
    done

    print_header "Generating Infrastructure Certificates"
    for service_config in "${INFRASTRUCTURE_SERVICES[@]}"; do
        IFS=':' read -r service_name port <<< "$service_config"
        generate_infrastructure_keystore "$service_name" "$port"
    done

    generate_test_clients
    create_documentation

    # Copy CA certificate
    cp temp/ca/ca-cert.pem keystores/service-ca-certificate.pem

    # Cleanup
    rm -rf temp

    # Summary
    echo
    print_success "🎉 Gateway HTTPS + Microservice mTLS setup completed!"
    echo
    print_header "Generated Structure"
    echo "📁 keystores/"
    echo "   ├── gateway/              (HTTPS for clients, mTLS client for services)"
    echo "   ├── microservices/        (mTLS required from gateway)"
    echo "   ├── infrastructure/       (HTTPS only)"
    echo "   ├── service-ca-certificate.pem"
    echo "   └── README.md"
    echo
    print_header "Key Benefits"
    echo "✅ External clients use simple HTTPS (no certificates)"
    echo "✅ Strong mTLS security between gateway and microservices"
    echo "✅ Microservices isolated from direct external access"
    echo "✅ Infrastructure services use lightweight HTTPS"
    echo
    print_info "Read keystores/README.md for detailed setup instructions!"
}

# Run main function
main "$@"