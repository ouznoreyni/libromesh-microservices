package sn.noreyni.integration.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sn.noreyni.userservice.UserServiceApplication;
import sn.noreyni.userservice.config.AsyncConfiguration;
import sn.noreyni.userservice.config.JacksonConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Integration test annotation with Keycloak Testcontainer setup.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        classes = {
                UserServiceApplication.class,
                JacksonConfiguration.class,
        }
)
@Testcontainers
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=#{@keycloakContainer.getAuthServerUrl()}/realms/jhipster",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=#{@keycloakContainer.getAuthServerUrl()}/realms/jhipster/protocol/openid-connect/certs",
        "spring.security.oauth2.client.registration.oidc.client-id=jhipster-app",
        "spring.security.oauth2.client.registration.oidc.client-secret=web_app",
        "spring.security.oauth2.client.provider.oidc.issuer-uri=#{@keycloakContainer.getAuthServerUrl()}/realms/jhipster"
})
public @interface IntegrationTest {
    // 5s is Spring's default
    String DEFAULT_TIMEOUT = "PT5S";
    String DEFAULT_ENTITY_TIMEOUT = "PT5S";

//    @Container
 //   @ServiceConnection
 /*   KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
            .withRealmImportFile("keycloak/realm-export.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withEnv("KC_FEATURES", "service-accounts-enabled")
            .withCommand("start-dev")
            .withExposedPorts(8080);*/
}