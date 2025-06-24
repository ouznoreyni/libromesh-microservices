/*
package sn.noreyni.integration.authenticaton;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import sn.noreyni.integration.config.TestcontainersConfiguration;
import sn.noreyni.userservice.authentication.AuthenticationService;
import sn.noreyni.userservice.authentication.dto.*;

import java.time.Duration;

@SpringBootTest(classes = {
        sn.noreyni.userservice.UserServiceApplication.class,
        sn.noreyni.userservice.config.KeycloakAdminClientConfig.class
})
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationServiceIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // This will start the container if not already started
        TestcontainersConfiguration.configureProperties(registry);
    }

    @BeforeAll
    static void setUp() {
        // Container should already be started by @DynamicPropertySource
        // Add a small delay to ensure Keycloak is fully ready
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterAll
    static void tearDown() {
        TestcontainersConfiguration.stopContainer();
    }

    @Test
    void testRegisterUser() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser1")
                .email("testuser1@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        Mono<RegisterResponse> registerMono = authenticationService.register(registerRequest);

        StepVerifier.create(registerMono)
                .expectNextMatches(response -> {
                    assert response.getUserId() != null : "User ID should not be null";
                    assert response.getCreatedAt() != null : "Created date should not be null";
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testLoginWithValidCredentials() {
        // First register a user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser2")
                .email("testuser2@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        // Register and then login
        Mono<LoginResponse> loginMono = authenticationService.register(registerRequest)
                .then(authenticationService.login(
                        LoginRequest.builder()
                                .username("testuser2")
                                .password("password123")
                                .build()
                ));

        StepVerifier.create(loginMono)
                .expectNextMatches(response -> {
                    assert response.getAccessToken() != null : "Access token should not be null";
                    assert response.getRefreshToken() != null : "Refresh token should not be null";
                    assert "Bearer".equals(response.getTokenType()) : "Token type should be Bearer";
                    assert response.getExpiresIn() != null : "Expires in should not be null";
                    assert response.getLoginTime() != null : "Login time should not be null";
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testLoginWithInvalidCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("nonexistentuser")
                .password("wrongpassword")
                .build();

        Mono<LoginResponse> loginMono = authenticationService.login(loginRequest);

        StepVerifier.create(loginMono)
                .expectError()
                .verify();
    }

    @Test
    void testRefreshToken() {
        // Register user first
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("refreshuser")
                .email("refreshuser@example.com")
                .password("password123")
                .firstName("Refresh")
                .lastName("User")
                .build();

        // Register, login, then refresh token
        Mono<RefreshTokenResponse> refreshMono = authenticationService.register(registerRequest)
                .then(authenticationService.login(
                        LoginRequest.builder()
                                .username("refreshuser")
                                .password("password123")
                                .build()
                ))
                .flatMap(loginResponse -> {
                    RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                            .refreshToken(loginResponse.getRefreshToken())
                            .build();
                    return authenticationService.refreshToken(refreshRequest);
                });

        StepVerifier.create(refreshMono)
                .expectNextMatches(response -> {
                    assert response.getAccessToken() != null : "New access token should not be null";
                    assert response.getRefreshToken() != null : "New refresh token should not be null";
                    assert "Bearer".equals(response.getTokenType()) : "Token type should be Bearer";
                    assert response.getRefreshedAt() != null : "Refresh time should not be null";
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testRefreshTokenWithInvalidToken() {
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("invalid_refresh_token")
                .build();

        Mono<RefreshTokenResponse> refreshMono = authenticationService.refreshToken(refreshRequest);

        StepVerifier.create(refreshMono)
                .expectError()
                .verify();
    }

    @Test
    void testGetUserInformationFromToken() {
        // Register and login first to get a valid token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("userinfouser")
                .email("userinfouser@example.com")
                .password("password123")
                .firstName("UserInfo")
                .lastName("Test")
                .build();

        Mono<UserInfo> userInfoMono = authenticationService.register(registerRequest)
                .then(authenticationService.login(
                        LoginRequest.builder()
                                .username("userinfouser")
                                .password("password123")
                                .build()
                ))
                .flatMap(loginResponse ->
                        authenticationService.getUserInformationFromToken(loginResponse.getAccessToken())
                );

        StepVerifier.create(userInfoMono)
                .expectNextMatches(userInfo -> {
                    assert "userinfouser".equals(userInfo.getUsername()) : "Username should match";
                    assert "userinfouser@example.com".equals(userInfo.getEmail()) : "Email should match";
                    assert "UserInfo".equals(userInfo.getFirstName()) : "First name should match";
                    assert "Test".equals(userInfo.getLastName()) : "Last name should match";
                    assert userInfo.getActive() != null : "Active status should not be null";
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testGetUserInformationWithInvalidToken() {
        Mono<UserInfo> userInfoMono = authenticationService.getUserInformationFromToken("invalid_token");

        StepVerifier.create(userInfoMono)
                .expectError()
                .verify();
    }

    @Test
    void testLogout() {
        // Register and login first to get tokens
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("logoutuser")
                .email("logoutuser@example.com")
                .password("password123")
                .firstName("Logout")
                .lastName("User")
                .build();

        Mono<Void> logoutMono = authenticationService.register(registerRequest)
                .then(authenticationService.login(
                        LoginRequest.builder()
                                .username("logoutuser")
                                .password("password123")
                                .build()
                ))
                .flatMap(loginResponse -> {
                    LogoutRequest logoutRequest = LogoutRequest.builder()
                            .refreshToken(loginResponse.getRefreshToken())
                            .build();
                    return authenticationService.logout(logoutRequest);
                });

        StepVerifier.create(logoutMono)
                .verifyComplete();
    }

    @Test
    void testRegisterUserWithDuplicateUsername() {
        RegisterRequest registerRequest1 = RegisterRequest.builder()
                .username("duplicateuser")
                .email("duplicate1@example.com")
                .password("password123")
                .firstName("Duplicate")
                .lastName("User1")
                .build();

        RegisterRequest registerRequest2 = RegisterRequest.builder()
                .username("duplicateuser") // Same username
                .email("duplicate2@example.com")
                .password("password123")
                .firstName("Duplicate")
                .lastName("User2")
                .build();

        // Register first user
        Mono<RegisterResponse> firstRegister = authenticationService.register(registerRequest1);

        // Try to register second user with same username
        Mono<RegisterResponse> secondRegister = firstRegister
                .then(authenticationService.register(registerRequest2));

        StepVerifier.create(firstRegister)
                .expectNextMatches(response -> response.getUserId() != null)
                .verifyComplete();

        StepVerifier.create(secondRegister)
                .expectError()
                .verify();
    }

    @Test
    void testCompleteAuthenticationFlow() {
        // Test the complete flow: Register -> Login -> Refresh -> UserInfo -> Logout
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("completeflowuser")
                .email("completeflow@example.com")
                .password("password123")
                .firstName("Complete")
                .lastName("Flow")
                .build();

        Mono<String> completeMono = authenticationService.register(registerRequest)
                .then(authenticationService.login(
                        LoginRequest.builder()
                                .username("completeflowuser")
                                .password("password123")
                                .build()
                ))
                .flatMap(loginResponse -> {
                    // Test refresh token
                    RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                            .refreshToken(loginResponse.getRefreshToken())
                            .build();

                    return authenticationService.refreshToken(refreshRequest)
                            .flatMap(refreshResponse -> {
                                // Test get user info
                                return authenticationService.getUserInformationFromToken(refreshResponse.getAccessToken())
                                        .flatMap(userInfo -> {
                                            // Test logout
                                            LogoutRequest logoutRequest = LogoutRequest.builder()
                                                    .refreshToken(refreshResponse.getRefreshToken())
                                                    .build();

                                            return authenticationService.logout(logoutRequest)
                                                    .thenReturn("Complete flow successful");
                                        });
                            });
                });

        StepVerifier.create(completeMono)
                .expectNext("Complete flow successful")
                .verifyComplete();
    }
}*/
