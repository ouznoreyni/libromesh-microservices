package sn.noreyni.userservice.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
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

    public Mono<LoginResponse> login(LoginRequest request) {
        return executeWithLogging("login", request.getUsername(),
                authenticateWithKeycloak(request)
                        .map(this::toLoginResponse));
    }

    public Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        return executeWithLogging("refreshToken", null,
                buildWebClient()
                        .post()
                        .uri(tokenEndpoint())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData(buildRefreshTokenFormData(request)))
                        .retrieve()
                        .bodyToMono(KeycloakTokenResponse.class)
                        .map(this::toRefreshTokenResponse));
    }

    public Mono<Void> logout(LogoutRequest request) {
        return executeWithLogging("logout", null,
                buildWebClient()
                        .post()
                        .uri(logoutEndpoint())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData(buildLogoutFormData(request)))
                        .retrieve()
                        .bodyToMono(Void.class));
    }

    public Mono<UserInfo> getUserInformationFromToken(String accessToken) {
        return executeWithLogging("getUserInformationFromToken", null,
                buildWebClient()
                        .get()
                        .uri(userInfoEndpoint())
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(KeycloakUserInfo.class)
                        .map(this::toUserInfo));
    }

    public Mono<RegisterResponse> register(RegisterRequest request) {
        return executeWithLogging("register", request.getUsername(),
                Mono.fromCallable(() -> {
                    var realmResource = keycloak.realm(keycloakConfig.getRealm());
                    var user = createUserRepresentation(request);

                    var response = realmResource.users().create(user);
                    if (response.getStatus() >= 400) {
                        throw ApiException.badRequest("Failed to create user in Keycloak, status: " + response.getStatus());
                    }

                    String userId = extractUserId(response);
                    setUserPassword(realmResource, userId, request.getPassword());

                    return RegisterResponse.builder()
                            .userId(userId)
                            .createdAt(LocalDateTime.now())
                            .build();
                }));
    }

    private Mono<KeycloakTokenResponse> authenticateWithKeycloak(LoginRequest request) {
        return buildWebClient()
                .post()
                .uri(tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(buildLoginFormData(request)))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class);
    }

    private <T> Mono<T> executeWithLogging(String method, String username, Mono<T> operation) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("{} started | correlation_id={} | username={} | method={}",
                method, correlationId, username, method);

        return operation
                .doOnSuccess(response -> log.info("{} successful | correlation_id={} | username={} | method={} | status=success | duration_ms={}",
                        method, correlationId, username, method, System.currentTimeMillis() - startTime))
                .doOnError(error -> log.error("{} failed | correlation_id={} | username={} | method={} | status=error | error_code={} | error_message={} | duration_ms={}",
                        method, correlationId, username, method,
                        error instanceof ApiException  apiException? apiException.getCode() :
                                "UNKNOWN",
                        error.getMessage(), System.currentTimeMillis() - startTime))
                .onErrorMap(this::mapWebClientException);
    }

    private WebClient buildWebClient() {
        return webClientBuilder.build();
    }

    private String tokenEndpoint() {
        return keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/token";
    }

    private String logoutEndpoint() {
        return keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/logout";
    }

    private String userInfoEndpoint() {
        return keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/userinfo";
    }

    private MultiValueMap<String, String> buildLoginFormData(LoginRequest request) {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "password");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("username", request.getUsername());
        formData.add("password", request.getPassword());
        formData.add("scope", "openid profile email");
        return formData;
    }

    private MultiValueMap<String, String> buildRefreshTokenFormData(RefreshTokenRequest request) {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("refresh_token", request.getRefreshToken());
        return formData;
    }

    private MultiValueMap<String, String> buildLogoutFormData(LogoutRequest request) {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("client_id", keycloakConfig.getClientId());
        formData.add("client_secret", keycloakConfig.getClientSecret());
        formData.add("refresh_token", request.getRefreshToken());
        return formData;
    }

    private LoginResponse toLoginResponse(KeycloakTokenResponse tokenResponse) {
        return LoginResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .tokenType(tokenResponse.getTokenType())
                .expiresIn(tokenResponse.getExpiresIn())
                .refreshExpiresIn(tokenResponse.getRefreshExpiresIn())
                .loginTime(LocalDateTime.now())
                .build();
    }

    private RefreshTokenResponse toRefreshTokenResponse(KeycloakTokenResponse response) {
        return RefreshTokenResponse.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .tokenType(response.getTokenType())
                .expiresIn(response.getExpiresIn())
                .refreshExpiresIn(response.getRefreshExpiresIn())
                .refreshedAt(LocalDateTime.now())
                .build();
    }

    private UserInfo toUserInfo(KeycloakUserInfo userInfo) {
        return UserInfo.builder()
                .username(userInfo.getPreferredUsername())
                .email(userInfo.getEmail())
                .firstName(userInfo.getGivenName())
                .lastName(userInfo.getFamilyName())
                .emailVerified(userInfo.getEmailVerified())
                .active(true)
                .build();
    }

    private UserRepresentation createUserRepresentation(RegisterRequest request) {
        var user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        return user;
    }

    private String extractUserId(jakarta.ws.rs.core.Response response) {
        return response.getLocation().getPath().substring(response.getLocation().getPath().lastIndexOf('/') + 1);
    }

    private void setUserPassword(RealmResource realmResource, String userId, String password) {
        var passwordCred = new CredentialRepresentation();
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(password);
        passwordCred.setTemporary(false);
        realmResource.users().get(userId).resetPassword(passwordCred);
    }

    private Throwable mapWebClientException(Throwable ex) {
        if (ex instanceof WebClientResponseException webEx) {
            log.error("WebClient error | status_code={} | response_body={}",
                    webEx.getStatusCode(), webEx.getResponseBodyAsString());
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