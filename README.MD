# LibroMesh - Library Management Platform

> Modern Library Management System built with Cloud-Native Microservices Architecture

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Keycloak](https://img.shields.io/badge/Keycloak-23.0.3-blue.svg)](https://www.keycloak.org/)
[![Vault](https://img.shields.io/badge/HashiCorp%20Vault-1.15.2-purple.svg)](https://www.vaultproject.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326ce5.svg)](https://kubernetes.io/)

## 🏗️ Architecture Overview

LibroMesh is a comprehensive library management platform built using modern microservices architecture with enterprise-grade security, configuration management, and DevOps practices.

```
┌─────────────────────────────────────────────────────────────────┐
│                     Infrastructure Layer                        │
├─────────────────┬───────────────────┬───────────────────────────┤
│   HashiCorp     │     Keycloak      │        PostgreSQL         │
│     Vault       │  (Authentication) │      (Database)           │
│  (Port 8200)    │   (Port 8180)     │      (Port 5432)         │
└─────────────────┴───────────────────┴───────────────────────────┘
         │                   │                       │
┌─────────────────────────────────────────────────────────────────┐
│                   Gateway & Discovery Layer                     │
├─────────────────┬───────────────────┬───────────────────────────┤
│   Discovery     │   API Gateway     │     Message Queue         │
│   Service       │    Service        │     (KAFKA)           │
│   (Port 8761)   │   (Port 8080)     │    (Port 5672)           │
└─────────────────┴───────────────────┴───────────────────────────┘
         │                   │                       │
┌─────────────────────────────────────────────────────────────────┐
│                  Business Services Layer                        │
├─────────────────┬───────────────────┬───────────────────────────┤
│   User Service  │   Book Service    │   Borrowing Service       │
│   (Port 8081)   │   (Port 8082)     │   (Port 8083)            │
├─────────────────┼───────────────────┼───────────────────────────┤
│ Inventory Svc   │ Notification Svc  │   Report Service          │
│ (Port 8086)     │   (Port 8084)     │   (Port 8085)            │
└─────────────────┴───────────────────┴───────────────────────────┘
```

## 📁 Project Structure

```
libromesh/
├── 📁 infrastructure/              # Infrastructure & DevOps
│   ├── docker-compose.yml          # Local development environment
│   ├── k8s/                        # Kubernetes manifests
│   ├── jenkins/                    # CI/CD pipeline
│   ├── monitoring/                 # Prometheus & Grafana
│   └── vault/                      # Vault configuration
├── 📁 services/                    # Microservices
│   ├── 📁 discovery-service/       # Eureka Service Discovery
│   ├── 📁 api-gateway/            # Spring Cloud Gateway
│   ├── 📁 user-service/           # User Management
│   ├── 📁 book-service/           # Book Catalog
│   ├── 📁 borrowing-service/      # Borrowing Operations
│   ├── 📁 inventory-service/      # Inventory Management
│   ├── 📁 notification-service/   # Notifications
│   └── 📁 report-service/         # Reports & Analytics
├── 📁 shared/                     # Shared libraries
│   ├── 📁 common/                 # Common utilities
│   ├── 📁 security/               # Security configurations
│   └── 📁 messaging/              # Message handling
├── 📄 pom.xml                     # Parent POM
└── 📄 docker-compose.yml          # Quick start environment
```

## 🚀 Quick Start

### Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Git**

### 1. Clone the Repository

[//]: # (```bash)

[//]: # (git clone https://github.com/yourusername/libromesh.git)

[//]: # (cd libromesh)

[//]: # (```)

[//]: # ()
[//]: # (### 2. Start Infrastructure Services)

[//]: # ()
[//]: # (```bash)

[//]: # (# Start infrastructure &#40;Vault, Keycloak, Database&#41;)

[//]: # (docker-compose up -d vault keycloak postgres redis kafka)

[//]: # ()
[//]: # (# Wait for services to be ready &#40;2-3 minutes&#41;)

[//]: # (docker-compose logs -f vault keycloak)

[//]: # (```)

[//]: # ()
[//]: # (### 3. Initialize Vault Configuration)

[//]: # ()
[//]: # (```bash)

[//]: # (# Initialize Vault with LibroMesh configuration)

[//]: # (./scripts/init-vault.sh)

[//]: # ()
[//]: # (# Verify Vault is configured)

[//]: # (curl -H "X-Vault-Token: root-token" http://localhost:8200/v1/secret/data/libromesh/database)

[//]: # (```)

[//]: # ()
[//]: # (### 4. Configure Keycloak)

[//]: # ()
[//]: # (```bash)

[//]: # (# Import LibroMesh realm configuration)

[//]: # (./scripts/setup-keycloak.sh)

[//]: # ()
[//]: # (# Or manually:)

[//]: # (# 1. Access http://localhost:8180)

[//]: # (# 2. Login with admin/admin123)

[//]: # (# 3. Import realm from ./keycloak/libromesh-realm.json)

[//]: # (```)

[//]: # ()
[//]: # (### 5. Build and Start Services)

[//]: # ()
[//]: # (```bash)

[//]: # (# Build all services)

[//]: # (mvn clean install)

[//]: # ()
[//]: # (# Start core services first)

[//]: # (docker-compose up -d discovery-service api-gateway)

[//]: # ()
[//]: # (# Start business services)

[//]: # (docker-compose up -d user-service book-service borrowing-service)

[//]: # ()
[//]: # (# Start supporting services)

[//]: # (docker-compose up -d inventory-service notification-service report-service)

[//]: # (```)

[//]: # ()
[//]: # (### 6. Verify Deployment)

[//]: # ()
[//]: # (```bash)

[//]: # (# Check service discovery)

[//]: # (curl http://localhost:8761)

[//]: # ()
[//]: # (# Check API Gateway)

[//]: # (curl http://localhost:8080/actuator/health)

[//]: # ()
[//]: # (# Test authentication)

[//]: # (curl -X POST http://localhost:8080/api/auth/login \)

[//]: # (  -H "Content-Type: application/json" \)

[//]: # (  -d '{"username":"admin","password":"admin123"}')

[//]: # (```)

[//]: # ()
[//]: # (## 🏗️ Module Architecture)

[//]: # ()
[//]: # (### Multi-Module Maven Structure)

[//]: # ()
[//]: # (The project follows a multi-module Maven structure with shared libraries and individual microservices. Each module has specific responsibilities and can be developed, tested, and deployed independently.)

[//]: # ()
[//]: # (### Shared Modules)

[//]: # ()
[//]: # (#### Common Module)

[//]: # (Contains shared utilities, DTOs, exception handlers, and constants used across all services.)

[//]: # ()
[//]: # (#### Security Module)

[//]: # (Provides shared security configuration, JWT utilities, and Keycloak integration components.)

[//]: # ()
[//]: # (#### Messaging Module)

[//]: # (Handles event publishing, message queue configuration, and inter-service communication patterns.)

[//]: # ()
[//]: # (### Service Modules)

[//]: # ()
[//]: # (Each microservice is implemented as a separate Maven module with its own dependencies, configuration, and lifecycle management.)

[//]: # ()
[//]: # (## 🔧 Service Architecture)

[//]: # ()
[//]: # (### Infrastructure Services)

[//]: # ()
[//]: # (#### Discovery Service)

[//]: # (Eureka-based service registry enabling service discovery and load balancing across the microservices ecosystem.)

[//]: # ()
[//]: # (#### API Gateway)

[//]: # (Spring Cloud Gateway providing unified entry point, request routing, authentication, and cross-cutting concerns.)

[//]: # ()
[//]: # (### Business Services)

[//]: # ()
[//]: # (#### User Service)

[//]: # (Manages user accounts, authentication, authorization, and user profile information with role-based access control.)

[//]: # ()
[//]: # (#### Book Service)

[//]: # (Handles book catalog management, categories, search functionality, and book metadata operations.)

[//]: # ()
[//]: # (#### Borrowing Service)

[//]: # (Core library operations including book borrowing, returns, renewals, reservations, and due date management.)

[//]: # ()
[//]: # (#### Inventory Service)

[//]: # (Tracks physical book copies, stock levels, location management, and acquisition processes.)

[//]: # ()
[//]: # (#### Notification Service)

[//]: # (Handles email and SMS notifications for due dates, overdue items, reservations, and system alerts.)

[//]: # ()
[//]: # (#### Report Service)

[//]: # (Generates analytics, reports, and business intelligence dashboards for library operations and usage patterns.)

[//]: # ()
[//]: # (## 🔐 Security & Configuration)

[//]: # ()
[//]: # (### Keycloak Integration)

[//]: # ()
[//]: # (**Authentication & Authorization**)

[//]: # (- OAuth2 and OpenID Connect integration)

[//]: # (- Role-based access control &#40;RBAC&#41;)

[//]: # (- Multi-tenant support with realm configuration)

[//]: # (- SSO capabilities across all services)

[//]: # ()
[//]: # (**Default Access Levels**)

[//]: # (- **Admin**: Full system administration and configuration)

[//]: # (- **Librarian**: Library operations, book management, borrowing oversight)

[//]: # (- **Member**: Personal account management, book browsing, borrowing operations)

[//]: # ()
[//]: # (### HashiCorp Vault Configuration)

[//]: # ()
[//]: # (**Secrets Management**)

[//]: # (- Database credentials and connection strings)

[//]: # (- Service-to-service authentication tokens)

[//]: # (- API keys and external service credentials)

[//]: # (- Encryption keys and certificates)

[//]: # ()
[//]: # (**Configuration Management**)

[//]: # (- Environment-specific configurations)

[//]: # (- Feature flags and operational parameters)

[//]: # (- Dynamic configuration updates without restarts)

[//]: # (- Audit logging for configuration changes)

[//]: # ()
[//]: # (## 🚀 API Overview)

[//]: # ()
[//]: # (### Core Endpoints)

[//]: # ()
[//]: # (#### Authentication APIs)

[//]: # (User login, logout, token refresh, and account management operations.)

[//]: # ()
[//]: # (#### User Management APIs)

[//]: # (Complete user lifecycle management including registration, profile updates, and administrative operations.)

[//]: # ()
[//]: # (#### Book Management APIs)

[//]: # (Book catalog operations, search functionality, category management, and inventory tracking.)

[//]: # ()
[//]: # (#### Borrowing APIs)

[//]: # (Library circulation operations including borrowing, returns, renewals, and reservation management.)

[//]: # ()
[//]: # (#### Reporting APIs)

[//]: # (Analytics and reporting endpoints for operational insights and business intelligence.)

[//]: # ()
[//]: # (## 🐳 Containerization & Orchestration)

[//]: # ()
[//]: # (### Docker Implementation)

[//]: # ()
[//]: # (Each service is containerized with optimized multi-stage builds, health checks, and security best practices.)

[//]: # ()
[//]: # (### Docker Compose)

[//]: # ()
[//]: # (Local development environment with all required infrastructure services and application services orchestrated together.)

[//]: # ()
[//]: # (### Kubernetes Ready)

[//]: # ()
[//]: # (Complete Kubernetes manifests including deployments, services, ingress, config maps, and secrets for production deployment.)

[//]: # ()
[//]: # (## 📊 Monitoring & Observability)

[//]: # ()
[//]: # (### Health Monitoring)

[//]: # ()
[//]: # (Comprehensive health checks at multiple levels including application health, database connectivity, and external service dependencies.)

[//]: # ()
[//]: # (### Metrics & Monitoring)

[//]: # ()
[//]: # (Integration with Prometheus for metrics collection and Grafana for visualization and alerting.)

[//]: # ()
[//]: # (### Distributed Tracing)

[//]: # ()
[//]: # (Request tracing across microservices for performance monitoring and debugging complex interactions.)

[//]: # ()
[//]: # (### Logging)

[//]: # ()
[//]: # (Centralized logging with structured logging patterns and correlation IDs for request tracking.)

[//]: # ()
[//]: # (## 🧪 Testing Strategy)

[//]: # ()
[//]: # (### Unit Testing)

[//]: # ()
[//]: # (Comprehensive unit tests for all service layers with high code coverage requirements.)

[//]: # ()
[//]: # (### Integration Testing)

[//]: # ()
[//]: # (Service-level integration tests including database integration and message queue interactions.)

[//]: # ()
[//]: # (### Contract Testing)

[//]: # ()
[//]: # (API contract testing to ensure service compatibility and prevent breaking changes.)

[//]: # ()
[//]: # (### End-to-End Testing)

[//]: # ()
[//]: # (Complete workflow testing across multiple services for critical business processes.)

[//]: # ()
[//]: # (## 🚀 CI/CD Pipeline)

[//]: # ()
[//]: # (### Continuous Integration)

[//]: # ()
[//]: # (Automated build, test, and security scanning pipeline with quality gates and code coverage requirements.)

[//]: # ()
[//]: # (### Continuous Deployment)

[//]: # ()
[//]: # (GitOps-based deployment using ArgoCD with automated promotion across environments.)

[//]: # ()
[//]: # (### Environment Management)

[//]: # ()
[//]: # (Separate environments for development, staging, and production with environment-specific configurations.)

[//]: # ()
[//]: # (### Security Scanning)

[//]: # ()
[//]: # (Integrated security scanning for dependencies, container images, and infrastructure configurations.)

[//]: # ()
[//]: # (## 🛠️ Development Guidelines)

[//]: # ()
[//]: # (### Code Standards)

[//]: # ()
[//]: # (Consistent coding standards across all services with automated formatting and linting.)

[//]: # ()
[//]: # (### Service Communication)

[//]: # ()
[//]: # (Standardized patterns for synchronous and asynchronous communication between services.)

[//]: # ()
[//]: # (### Database Design)

[//]: # ()
[//]: # (Database-per-service pattern with proper data consistency and transaction management strategies.)

[//]: # ()
[//]: # (### Error Handling)

[//]: # ()
[//]: # (Standardized error response formats and centralized exception handling across all services.)

[//]: # ()
[//]: # (## 📚 Business Features)

[//]: # ()
[//]: # (### Library Operations)

[//]: # ()
[//]: # (Complete library management including cataloging, circulation, inventory management, and member services.)

[//]: # ()
[//]: # (### Advanced Features)

[//]: # ()
[//]: # (- Multi-location support for library branches)

[//]: # (- Integration with external library systems)

[//]: # (- Digital resource management)

[//]: # (- Fine and fee management)

[//]: # (- Automated notifications and reminders)

[//]: # ()
[//]: # (### Reporting & Analytics)

[//]: # ()
[//]: # (Comprehensive reporting on library usage, popular books, member activity, and operational metrics.)

[//]: # ()
[//]: # (## 🌐 Scalability & Performance)

[//]: # ()
[//]: # (### Horizontal Scaling)

[//]: # ()
[//]: # (Services designed for horizontal scaling with stateless architecture and external session management.)

[//]: # ()
[//]: # (### Caching Strategy)

[//]: # ()
[//]: # (Multi-level caching including application-level caching, database query caching, and CDN integration.)

[//]: # ()
[//]: # (### Database Optimization)

[//]: # ()
[//]: # (Optimized database design with proper indexing, query optimization, and connection pooling.)

[//]: # ()
[//]: # (### Load Balancing)

[//]: # ()
[//]: # (Intelligent load balancing at multiple levels including API Gateway and service-to-service communication.)

[//]: # ()
[//]: # (## 🔄 Data Management)

[//]: # ()
[//]: # (### Data Consistency)

[//]: # ()
[//]: # (Event-driven architecture with eventual consistency patterns and saga pattern for distributed transactions.)

[//]: # ()
[//]: # (### Backup & Recovery)

[//]: # ()
[//]: # (Automated backup strategies with point-in-time recovery capabilities and disaster recovery procedures.)

[//]: # ()
[//]: # (### Data Migration)

[//]: # ()
[//]: # (Versioned database migrations with rollback capabilities and zero-downtime deployment support.)

[//]: # ()
[//]: # (## 🤝 Contributing)

[//]: # ()
[//]: # (We welcome contributions from the community. Please read our contributing guidelines and code of conduct before submitting pull requests.)

[//]: # ()
[//]: # (## 📄 License)

[//]: # ()
[//]: # (This project is licensed under the MIT License - see the LICENSE file for details.)

[//]: # ()
[//]: # (## 🆘 Support & Documentation)

[//]: # ()
[//]: # (- **GitHub Issues**: Bug reports and feature requests)

[//]: # (- **Wiki**: Detailed documentation and guides)

[//]: # (- **Discussions**: Community support and questions)

[//]: # ()
[//]: # (## 🎯 Roadmap)

[//]: # ()
[//]: # (- **Phase 1**: Core library management features)

[//]: # (- **Phase 2**: Advanced analytics and reporting)

[//]: # (- **Phase 3**: Mobile application and API enhancements)

[//]: # (- **Phase 4**: AI-powered recommendations and automation)

[//]: # (- **Phase 5**: Blockchain integration for digital assets)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (**Built with ❤️ for the library community**)

[//]: # ()
[//]: # (> *LibroMesh - Where traditional library meets modern technology*)