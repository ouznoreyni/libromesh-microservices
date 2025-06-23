package sn.noreyni.userservice.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import sn.noreyni.userservice.authentication.dto.*;
import sn.noreyni.userservice.config.KeycloakAdminClientConfig;
import sn.noreyni.userservice.exception.ApiException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final Keycloak keycloak;
    private final WebClient.Builder webClientBuilder;
    private final KeycloakAdminClientConfig keycloakConfig;

    /**
     * Authenticate user and return login information
     */
    public Mono<LoginResponse> login(LoginRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Login attempt started | correlation_id={} | username={} | method=login",
                correlationId, request.getUsername());

        return authenticateWithKeycloak(request)
                .map(tokenResponse -> LoginResponse.builder()
                        .accessToken(tokenResponse.getAccessToken())
                        .refreshToken(tokenResponse.getRefreshToken())
                        .tokenType(tokenResponse.getTokenType())
                        .expiresIn(tokenResponse.getExpiresIn())
                        .refreshExpiresIn(tokenResponse.getRefreshExpiresIn())
                        .loginTime(LocalDateTime.now())
                        .build())
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Login successful | correlation_id={} | username={} | method=login | status=success | duration_ms={}",
                            correlationId, request.getUsername(), duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorCode = error instanceof ApiException ? ((ApiException) error).getCode() :
                            "UNKNOWN";
                    log.error("Login failed | correlation_id={} | username={} | method=login | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, request.getUsername(), errorCode, error.getMessage(), duration);
                })
                .onErrorMap(this::mapWebClientException);
    }

    /**
     * Refresh access token
     */
    public Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Token refresh attempt started | correlation_id={} | method=refreshToken", correlationId);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("refresh_token", request.getRefreshToken());

        return webClientBuilder.build()
                .post()
                .uri(keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .map(response -> RefreshTokenResponse.builder()
                        .accessToken(response.getAccessToken())
                        .refreshToken(response.getRefreshToken())
                        .tokenType(response.getTokenType())
                        .expiresIn(response.getExpiresIn())
                        .refreshExpiresIn(response.getRefreshExpiresIn())
                        .refreshedAt(LocalDateTime.now())
                        .build())
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Token refresh successful | correlation_id={} | method=refreshToken | status=success | duration_ms={}",
                            correlationId, duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorCode = error instanceof ApiException ?
                            ((ApiException) error).getCode() : "UNKNOWN";
                    log.error("Token refresh failed | correlation_id={} | method=refreshToken | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, errorCode, error.getMessage(), duration);
                })
                .onErrorMap(this::mapWebClientException);
    }

    /**
     * Logout user
     */
    public Mono<Void> logout(LogoutRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Logout attempt started | correlation_id={} | method=logout", correlationId);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("refresh_token", request.getRefreshToken());

        return webClientBuilder.build()
                .post()
                .uri(keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/logout")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Logout successful | correlation_id={} | method=logout | status=success | duration_ms={}",
                            correlationId, duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorCode = error instanceof ApiException ?
                            ((ApiException) error).getCode() : "UNKNOWN";
                    log.error("Logout failed | correlation_id={} | method=logout | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, errorCode, error.getMessage(), duration);
                })
                .onErrorMap(this::mapWebClientException);
    }

    /**
     * Get current user information from access token
     */
    public Mono<UserInfo> getUserInformationFromToken(String accessToken) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User info retrieval started | correlation_id={} | method=getUserInformationFromToken", correlationId);

        return webClientBuilder.build()
                .get()
                .uri(keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KeycloakUserInfo.class)
                .map(userInfo -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User info retrieved | correlation_id={} | username={} | method=getUserInformationFromToken | status=success | duration_ms={}",
                            correlationId, userInfo.getPreferredUsername(), duration);
                    return UserInfo.builder()
                            .username(userInfo.getPreferredUsername())
                            .email(userInfo.getEmail())
                            .firstName(userInfo.getGivenName())
                            .lastName(userInfo.getFamilyName())
                            .emailVerified(userInfo.getEmailVerified())
                            .active(true)
                            .build();
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorCode = error instanceof ApiException ? ((ApiException) error).getCode() : "UNKNOWN";
                    log.error("User info retrieval failed | correlation_id={} | method=getUserInformationFromToken | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, errorCode, error.getMessage(), duration);
                })
                .onErrorMap(this::mapWebClientException);
    }


    /**
     * Authenticate with Keycloak using Resource Owner Password Credentials flow
     */
    private Mono<KeycloakTokenResponse> authenticateWithKeycloak(LoginRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Keycloak authentication started | correlation_id={} | username={} | method=authenticateWithKeycloak",
                correlationId, request.getUsername());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("username", request.getUsername());
        formData.add("password", request.getPassword());
        formData.add("scope", "openid profile email");

        return webClientBuilder.build()
                .post()
                .uri(keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Keycloak authentication successful | correlation_id={} | username={} | method=authenticateWithKeycloak | status=success | duration_ms={}",
                            correlationId, request.getUsername(), duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Keycloak authentication failed | correlation_id={} | username={} | method=authenticateWithKeycloak | status=error |  | error_message={} | duration_ms={}",
                            correlationId, request.getUsername(), error.getMessage(),
                            duration);
                });
    }

    /**
     * Map WebClient exceptions to appropriate business exceptions
     */
    private Throwable mapWebClientException(Throwable ex) {
        if (ex instanceof WebClientResponseException webEx) {
            log.error("WebClient error | status_code={} | response_body={}", webEx.getStatusCode(), webEx.getResponseBodyAsString());

            return switch (webEx.getStatusCode().value()) {
                case 401 -> ApiException.authenticationFailed();
                case 400 -> ApiException.badRequest("Invalid request parameters");
                case 503 -> ApiException.serviceUnavailable("Keycloak");
                default -> ApiException.internalError("Authentication service error");
            };
        }
        return ApiException.internalError("Unexpected authentication error");
    }
}