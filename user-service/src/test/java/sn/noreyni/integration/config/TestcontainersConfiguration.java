package sn.noreyni.integration.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

public class TestcontainersConfiguration {

    private static GenericContainer<?> keycloakContainer;
    private static final String TEST_REALM = "test-realm";
    private static final String TEST_CLIENT_ID = "test-client";
    private static final String TEST_CLIENT_SECRET = "test-secret";

    public static void startContainer() {
        if (keycloakContainer == null || !keycloakContainer.isRunning()) {
            keycloakContainer = new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:26.2"))
                    .withExposedPorts(8080)
                    .withEnv("KEYCLOAK_ADMIN", "admin")
                    .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
                    .withEnv("KC_HTTP_ENABLED", "true")
                    .withEnv("KC_HOSTNAME_STRICT", "false")
                    .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
                    .withCommand("start-dev")
                    .waitingFor(Wait.forHttp("/realms/master")
                            .forStatusCode(200)
                            .withStartupTimeout(java.time.Duration.ofMinutes(3)));

            keycloakContainer.start();

            // Wait a bit more for Keycloak to be fully ready
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            setupTestRealm();
        }
    }

    private static void setupTestRealm() {
        String keycloakUrl = "http://localhost:" + keycloakContainer.getMappedPort(8080);

        System.out.println("Setting up test realm at: " + keycloakUrl);

        try {
            Keycloak adminClient = KeycloakBuilder.builder()
                    .serverUrl(keycloakUrl)
                    .realm("master")
                    .username("admin")
                    .password("admin")
                    .clientId("admin-cli")
                    .build();

            // Test admin client connection
            adminClient.serverInfo().getInfo();
            System.out.println("Successfully connected to Keycloak admin");

            // Create test realm
            RealmRepresentation realm = new RealmRepresentation();
            realm.setRealm(TEST_REALM);
            realm.setEnabled(true);
            realm.setRegistrationAllowed(true);
            realm.setLoginWithEmailAllowed(true);
            realm.setDuplicateEmailsAllowed(false);

            try {
                adminClient.realms().create(realm);
                System.out.println("Created test realm: " + TEST_REALM);
            } catch (Exception e) {
                System.out.println("Test realm already exists or failed to create: " + e.getMessage());
            }

            // Create test client
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(TEST_CLIENT_ID);
            client.setSecret(TEST_CLIENT_SECRET);
            client.setEnabled(true);
            client.setDirectAccessGrantsEnabled(true); // Enable direct access grants (password flow)
            client.setServiceAccountsEnabled(false);
            client.setPublicClient(false); // Confidential client
            client.setStandardFlowEnabled(true);
            client.setImplicitFlowEnabled(false);
            client.setRedirectUris(List.of("*"));
            client.setWebOrigins(List.of("*"));

            try {
                adminClient.realm(TEST_REALM).clients().create(client);
                System.out.println("Created test client: " + TEST_CLIENT_ID);
            } catch (Exception e) {
                System.out.println("Test client already exists or failed to create: " + e.getMessage());
            }

            adminClient.close();

        } catch (Exception e) {
            System.err.println("Failed to setup test realm: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to setup Keycloak test realm", e);
        }
    }

    public static void stopContainer() {
        if (keycloakContainer != null && keycloakContainer.isRunning()) {
            keycloakContainer.stop();
        }
    }

    public static void configureProperties(DynamicPropertyRegistry registry) {
        // Start container here if not already started
        if (keycloakContainer == null || !keycloakContainer.isRunning()) {
            startContainer();
        }

        String keycloakUrl = "http://localhost:" + keycloakContainer.getMappedPort(8080);

        System.out.println("Configuring properties with Keycloak URL: " + keycloakUrl);

        // Configure properties to match your KeycloakAdminClientConfig
        registry.add("app.keycloak.admin.server-url", () -> keycloakUrl);
        registry.add("app.keycloak.admin.realm", () -> "master"); // Use master for admin operations
        registry.add("app.keycloak.admin.client-id", () -> "admin-cli");
        registry.add("app.keycloak.admin.client-secret", () -> "");
        registry.add("app.keycloak.admin.username", () -> "admin");
        registry.add("app.keycloak.admin.password", () -> "admin");

        // Configure service properties (these are what your AuthenticationService uses)
        registry.add("keycloak.server-url", () -> keycloakUrl);
        registry.add("keycloak.realm", () -> TEST_REALM); // Service uses test realm
        registry.add("keycloak.client-id", () -> TEST_CLIENT_ID);
        registry.add("keycloak.client-secret", () -> TEST_CLIENT_SECRET);
    }
}