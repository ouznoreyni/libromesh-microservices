#!/bin/bash

# File: scripts/init-vault.sh
# Vault Initialization Script for LibroMesh

# Vault Initialization Script for LibroMesh
# This script initializes HashiCorp Vault with LibroMesh configurations

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

# Create LibroMesh database configuration
create_database_config() {
    log_info "Creating database configuration..."

    cat > /tmp/db_config.json << EOF
{
  "data": {
    "url": "jdbc:postgresql://postgres:5432/libromesh",
    "username": "libromesh_user",
    "password": "libromesh_password_123",
    "driver-class-name": "org.postgresql.Driver",
    "hikari": {
      "maximum-pool-size": 20,
      "minimum-idle": 5,
      "connection-timeout": 30000,
      "idle-timeout": 600000,
      "max-lifetime": 1800000
    },
    "jpa": {
      "hibernate": {
        "ddl-auto": "validate"
      },
      "show-sql": false,
      "properties": {
        "hibernate": {
          "dialect": "org.hibernate.dialect.PostgreSQLDialect",
          "format_sql": true
        }
      }
    }
  }
}
EOF

    curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
         -H "Content-Type: application/json" \
         -X POST \
         -d @/tmp/db_config.json \
         "$VAULT_ADDR/v1/secret/data/libromesh/database"

    rm /tmp/db_config.json
    log_info "Database configuration created"
}

# Create Keycloak configuration
create_keycloak_config() {
    log_info "Creating Keycloak configuration..."

    cat > /tmp/keycloak_config.json << EOF
{
  "data": {
    "auth-server-url": "http://keycloak:8180",
    "realm": "libromesh",
    "resource": "libromesh-client",
    "ssl-required": "external",
    "use-resource-role-mappings": true,
    "confidential-port": 0,
    "credentials": {
      "secret": "your-keycloak-client-secret"
    },
    "policy-enforcer": {
      "enable": true
    }
  }
}
EOF

    curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
         -H "Content-Type: application/json" \
         -X POST \
         -d @/tmp/keycloak_config.json \
         "$VAULT_ADDR/v1/secret/data/libromesh/keycloak"

    rm /tmp/keycloak_config.json
    log_info "Keycloak configuration created"
}

# Create messaging configuration
create_messaging_config() {
    log_info "Creating messaging configuration..."

    cat > /tmp/messaging_config.json << EOF
{
  "data": {
    "kafka": {
      "bootstrap-servers": "kafka:29092",
      "consumer": {
        "group-id": "libromesh-group",
        "auto-offset-reset": "earliest",
        "key-deserializer": "org.apache.kafka.common.serialization.StringDeserializer",
        "value-deserializer": "org.springframework.kafka.support.serializer.JsonDeserializer",
        "properties": {
          "spring.json.trusted.packages": "*"
        }
      },
      "producer": {
        "key-serializer": "org.apache.kafka.common.serialization.StringSerializer",
        "value-serializer": "org.springframework.kafka.support.serializer.JsonSerializer"
      }
    },
    "redis": {
      "host": "redis",
      "port": 6379,
      "password": "redis_password_123",
      "timeout": "2000ms",
      "jedis": {
        "pool": {
          "max-active": 8,
          "max-wait": "-1ms",
          "max-idle": 8,
          "min-idle": 0
        }
      }
    }
  }
}
EOF

    curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
         -H "Content-Type: application/json" \
         -X POST \
         -d @/tmp/messaging_config.json \
         "$VAULT_ADDR/v1/secret/data/libromesh/messaging"

    rm /tmp/messaging_config.json
    log_info "Messaging configuration created"
}

# Create service-specific configurations
create_service_configs() {
    log_info "Creating service-specific configurations..."

    # Services array
    services=("user-service" "book-service" "borrowing-service" "inventory-service" "notification-service" "report-service" "api-gateway" "discovery-service")
    ports=(8081 8082 8083 8086 8084 8085 8080 8761)

    for i in "${!services[@]}"; do
        service="${services[$i]}"
        port="${ports[$i]}"

        log_info "Creating configuration for $service..."

        cat > /tmp/${service}_config.json << EOF
{
  "data": {
    "server": {
      "port": $port
    },
    "spring": {
      "application": {
        "name": "$service"
      },
      "jpa": {
        "hibernate": {
          "ddl-auto": "validate"
        },
        "show-sql": false
      }
    },
    "management": {
      "endpoints": {
        "web": {
          "exposure": {
            "include": "health,info,metrics,prometheus"
          }
        }
      },
      "endpoint": {
        "health": {
          "show-details": "always"
        }
      },
      "metrics": {
        "export": {
          "prometheus": {
            "enabled": true
          }
        }
      }
    },
    "logging": {
      "level": {
        "com.libromesh": "INFO",
        "org.springframework.cloud": "DEBUG"
      }
    },
    "eureka": {
      "client": {
        "service-url": {
          "defaultZone": "http://discovery-service:8761/eureka/"
        },
        "register-with-eureka": true,
        "fetch-registry": true
      },
      "instance": {
        "prefer-ip-address": true,
        "instance-id": "\${spring.application.name}:\${server.port}"
      }
    }
  }
}
EOF

        # Add service-specific configurations
        case $service in
            "api-gateway")
                # Add gateway-specific config
                ;;
            "user-service")
                # Add user service specific config
                ;;
            "notification-service")
                # Add notification service specific config
                ;;
        esac

        curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
             -H "Content-Type: application/json" \
             -X POST \
             -d @/tmp/${service}_config.json \
             "$VAULT_ADDR/v1/secret/data/libromesh/$service"

        rm /tmp/${service}_config.json
        log_info "$service configuration created"
    done
}

# Create common application configuration
create_common_config() {
    log_info "Creating common application configuration..."

    cat > /tmp/common_config.json << EOF
{
  "data": {
    "libromesh": {
      "security": {
        "jwt": {
          "secret": "libromesh-jwt-secret-key-2024",
          "expiration": 3600000
        },
        "cors": {
          "allowed-origins": "*",
          "allowed-methods": "GET,POST,PUT,DELETE,OPTIONS",
          "allowed-headers": "*",
          "max-age": 3600
        }
      },
      "notification": {
        "email": {
          "smtp": {
            "host": "smtp.gmail.com",
            "port": 587,
            "username": "noreply@libromesh.com",
            "password": "your-email-password"
          }
        },
        "sms": {
          "provider": "twilio",
          "account-sid": "your-twilio-account-sid",
          "auth-token": "your-twilio-auth-token"
        }
      },
      "file": {
        "upload": {
          "max-size": "10MB",
          "allowed-types": "jpg,jpeg,png,pdf,doc,docx"
        }
      }
    }
  }
}
EOF

    curl -s -H "X-Vault-Token: $VAULT_TOKEN" \
         -H "Content-Type: application/json" \
         -X POST \
         -d @/tmp/common_config.json \
         "$VAULT_ADDR/v1/secret/data/libromesh/common"

    rm /tmp/common_config.json
    log_info "Common configuration created"
}

# Verify configurations
verify_configurations() {
    log_info "Verifying configurations..."

    paths=("database" "keycloak" "messaging" "common" "user-service" "book-service" "borrowing-service" "api-gateway")

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
    log_info "Starting LibroMesh Vault initialization..."

    check_vault_status
    enable_kv_engine
    create_database_config
    create_keycloak_config
    create_messaging_config
    create_service_configs
    create_common_config
    verify_configurations

    log_info "LibroMesh Vault initialization completed successfully!"
    log_info "You can now start your microservices."
}

# Run main function
main "$@"