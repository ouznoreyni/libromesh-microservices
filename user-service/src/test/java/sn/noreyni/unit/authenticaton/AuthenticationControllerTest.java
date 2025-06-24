package sn.noreyni.unit.authenticaton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import sn.noreyni.userservice.authentication.AuthenticationController;
import sn.noreyni.userservice.authentication.AuthenticationService;
import sn.noreyni.userservice.authentication.dto.*;
import sn.noreyni.userservice.common.ApiResponse;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(authenticationController).build();
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        RegisterResponse response = RegisterResponse.builder()
                .userId("123")
                .createdAt(LocalDateTime.now())
                .build();

        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<RegisterResponse>>() {})
                .consumeWith(result -> {
                    ApiResponse<RegisterResponse> apiResponse = result.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData().getUserId()).isEqualTo("123");
                    assertThat(apiResponse.getMessage()).isEqualTo("Utilisateur créé avec succès");
                });

        verify(authenticationService).register(any(RegisterRequest.class));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .refreshExpiresIn(7200)
                .loginTime(LocalDateTime.now())
                .build();

        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {})
                .consumeWith(result -> {
                    ApiResponse<LoginResponse> apiResponse = result.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData().getAccessToken()).isEqualTo("access-token");
                    assertThat(apiResponse.getMessage()).isEqualTo("Connexion réussie");
                });

        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    void testGetCurrentUserUnauthorized() {
        webTestClient.get()
                .uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<ApiResponse<?>>() {})
                .consumeWith(result -> {
                    ApiResponse<?> apiResponse = result.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getError().getCode()).isEqualTo("AUTH_001");
                    assertThat(apiResponse.getError().getMessage()).isEqualTo("En-tête d'autorisation manquant ou invalide");
                });
    }
}