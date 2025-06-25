package sn.noreyni.gateweyservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class GatewayOpenApiConfig {

    @Autowired
    private RouteDefinitionLocator locator;

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

    @Bean
    @Lazy(false)
    public List<AbstractSwaggerUiConfigProperties.SwaggerUrl> apis() {
        List<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new ArrayList<>();

        // Gateway service itself
        urls.add(new org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl(
                "Gateway Service",
                "/libromesh/api-docs",
                "LibroMesh Gateway Service API"
        ));

        // User Service via Gateway routing
        urls.add(new org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl(
                "User Service",
                "/user-service/libromesh/api-docs",
                "User Management & Authentication API"
        ));

        // Book Service via Gateway routing
        urls.add(new org.springdoc.core.properties.SwaggerUiConfigProperties.SwaggerUrl(
                "Book Service",
                "/book-service/libromesh/api-docs",
                "Book Management API"
        ));

        return urls;
    }
}