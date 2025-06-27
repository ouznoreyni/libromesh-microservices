package sn.noreyni.bookservice.config;

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
    public OpenAPI bookServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Book Service API - LibroMesh")
                        .description("""
                                The Book Service provides RESTful APIs to manage the library's book catalog,
                                including books, authors, publishers, categories,edit,review, and tags.
                                                                
                                Key Features:
                                - Add, update, and search books
                                - Manage authors and publishers
                                - Assign categories and tags
                                - Supports pagination, filtering, and validation
                                - etc
                                """)
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
                        .description("LibroMesh Platform Documentation & Source Code")
                        .url("https://github.com/ouznoreyni/libromesh-microservices"));
    }
}