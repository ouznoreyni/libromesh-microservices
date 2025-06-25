package sn.noreyni.gateweyservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@RequiredArgsConstructor
public class GatewayOpenApiConfig {

    private final RouteDefinitionLocator locator;

    private final ReactiveDiscoveryClient discoveryClient;

    // Thread-safe list for dynamic service discovery
    private final List<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrls = new CopyOnWriteArrayList<>();

    @Bean
    public OpenAPI gatewayServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LibroMesh API Gateway")
                        .description("""
                                **LibroMesh Gateway Service** - Central API Gateway for the LibroMesh Microservices Platform
                                
                                ## Overview
                                This gateway serves as the **single entry point** for all LibroMesh APIs, providing:
                                - **Unified API Access**: Route requests to appropriate microservices
                                - **Load Balancing**: Distribute traffic across service instances
                                - **Service Discovery**: Automatic routing to healthy service instances
                                - **Cross-Cutting Concerns**: Authentication, logging, and monitoring
                                
                                ## Available Services
                                - **User Service**: User management, authentication, and role-based access control
                                - **Book Service**: Book catalog management, search, and recommendations
                                - **Gateway Service**: API routing, health checks, and service coordination
                                
                                ## Architecture
                                ```
                                Client → Gateway (Port 8080) → [User Service | Book Service | Other Services]
                                ```
                                
                                ## Service Routing
                                - **User APIs**: `/user-service/**` → User Service
                                - **Book APIs**: `/book-service/**` → Book Service
                                - **Health Check**: `/actuator/health`
                                - **API Documentation**: `/libromesh/swagger-ui.html`
                                
                                ## Authentication
                                All requests are routed through the gateway, which handles:
                                - JWT token validation
                                - Role-based access control
                                - Request rate limiting
                                
                                ## Error Handling
                                The gateway provides standardized error responses across all services:
                                - **4xx**: Client errors (authentication, validation, etc.)
                                - **5xx**: Server errors (service unavailable, timeouts, etc.)
                                """)
                        .version("v1.0.0")
                        .termsOfService("https://github.com/ouznoreyni/libromesh-microservices/blob/main/TERMS.md")
                        .contact(new Contact()
                                .name("Ousmane DIOP")
                                .url("https://github.com/ouznoreyni/libromesh-microservices")
                                .email("ousmanediopp268@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .externalDocs(new ExternalDocumentation()
                        .description("📚 LibroMesh Project Documentation & Source Code")
                        .url("https://github.com/ouznoreyni/libromesh-microservices"))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server()
                        .url("http://localhost:9001")
                        .description("Local Development Server"))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server()
                        .url("https://api.libromesh.com")
                        .description("Production Server"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name("Gateway Health")
                        .description("Gateway service health and monitoring endpoints"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name("Service Discovery")
                        .description("Service discovery and routing information"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name("API Documentation")
                        .description("Centralized API documentation endpoints"));
    }

    @PostConstruct
    public void initializeSwaggerUrls() {
        // Gateway service itself (always available)
        swaggerUrls.add(new org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl(
                "Gateway Service",
                "/libromesh/api-docs",
                "LibroMesh Gateway Service API"
        ));

        // Dynamically discover services and add their Swagger URLs
        discoveryClient.getServices()
                .filter(serviceId -> !"gateway-service".equalsIgnoreCase(serviceId))
                .filter(serviceId -> !"eureka".equalsIgnoreCase(serviceId)) // Skip Eureka server
                .doOnNext(serviceId -> {
                    String displayName = formatServiceName(serviceId);
                    String swaggerUrl = "/" + serviceId + "/libromesh/api-docs";
                    String description = getServiceDescription(serviceId);

                    // Check if this service URL already exists
                    boolean exists = swaggerUrls.stream()
                            .anyMatch(url -> url.getUrl().equals(swaggerUrl));

                    if (!exists) {
                        swaggerUrls.add(new org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl(
                                displayName,
                                swaggerUrl,
                                description
                        ));
                        System.out.println("Added Swagger URL for service: " + serviceId + " -> " + swaggerUrl);
                    }
                })
                .doOnError(error -> System.err.println("Error discovering services: " + error.getMessage()))
                .subscribe();
    }

    @Bean
    @Lazy(false)
    public List<AbstractSwaggerUiConfigProperties.SwaggerUrl> apis() {
        return swaggerUrls;
    }

    /**
     * Formats service ID to display name
     * Example: "user-service" -> "User Service"
     */
    private String formatServiceName(String serviceId) {
        String[] words = serviceId.replace("-", " ").split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Provides meaningful descriptions for known services
     */
    private String getServiceDescription(String serviceId) {
        return switch (serviceId.toLowerCase()) {
            case "user-service" -> "User Management & Authentication API";
            case "book-service" -> "Book Management & Catalog API";
            case "notification-service" -> "Notification & Messaging API";
            case "order-service" -> "Order Management API";
            case "payment-service" -> "Payment Processing API";
            case "inventory-service" -> "Inventory Management API";
            case "review-service" -> "Review & Rating API";
            default -> serviceId.replace("-", " ").toUpperCase() + " API Documentation";
        };
    }
}