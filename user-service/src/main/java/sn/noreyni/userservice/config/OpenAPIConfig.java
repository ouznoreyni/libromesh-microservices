package sn.noreyni.userservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API - LibroMesh")
                        .description("APIs for managing users, roles, and authentication in LibroMesh platform.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Ousmane DIOP")
                                .url("https://github.com/ouznoreyni/libromesh-microservices")
                                .email("ousmanediopp268@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .externalDocs(new ExternalDocumentation()
                        .description("LibroMesh Documentation")
                        .url("https://github.com/ouznoreyni/libromesh-microservices"));
    }
}