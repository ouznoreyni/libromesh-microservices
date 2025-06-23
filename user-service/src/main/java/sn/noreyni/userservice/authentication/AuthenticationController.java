package sn.noreyni.userservice.authentication;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sn.noreyni.userservice.authentication.dto.*;
import sn.noreyni.userservice.common.ApiResponse;
import sn.noreyni.userservice.exception.ApiException;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<RegisterResponse>>> register(@Valid @RequestBody RegisterRequest request) {
        return handleRequest("register", request.getUsername(),
                authenticationService.register(request)
                        .map(response -> ApiResponse.<RegisterResponse>success(response, "Utilisateur créé avec succès")
                                .withMetadata("created_at", response.getCreatedAt().toString())));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> login(@Valid @RequestBody LoginRequest request) {
        return handleRequest("login", request.getUsername(),
                authenticationService.login(request)
                        .map(response -> ApiResponse.<LoginResponse>success(response, "Connexion réussie")
                                .withMetadata("login_time", response.getLoginTime().toString())));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<RefreshTokenResponse>>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return handleRequest("refreshToken", null,
                authenticationService.refreshToken(request)
                        .map(response -> ApiResponse.<RefreshTokenResponse>success(response, "Jeton rafraîchi avec succès")
                                .withMetadata("refreshed_at", response.getRefreshedAt().toString())));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(@Valid @RequestBody LogoutRequest request) {
        return handleRequest("logout", null,
                authenticationService.logout(request)
                        .thenReturn(ApiResponse.<Void>success(null, "Déconnexion réussie")
                                .withMetadata("logout_time", LocalDateTime.now().toString())));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<UserInfo>>> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH_001", "En-tête d'autorisation manquant ou invalide")));
        }

        return handleRequest("getCurrentUser", null,
                authenticationService.getUserInformationFromToken(extractToken(authHeader))
                        .map(response -> ApiResponse.success(response, "Informations utilisateur récupérées")));
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> handleRequest(String method, String username, Mono<ApiResponse<T>> operation) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("{} started | correlation_id={} | username={} | method={}", method, correlationId, username, method);

        return operation
                .map(response -> {
                    //response.setCorrelationId(correlationId);
                    log.info("{} successful | correlation_id={} | username={} | method={} | duration_ms={}",
                            method, correlationId, username, method, System.currentTimeMillis() - startTime);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    if (ex instanceof ApiException apiEx) {
                        log.error("{} failed | correlation_id={} | username={} | method={} | error_code={} | error_message={} | duration_ms={}",
                                method, correlationId, username, method, apiEx.getCode(), apiEx.getMessage(), duration);
                        return Mono.just(ResponseEntity.status(apiEx.getStatus())
                                .body(ApiResponse.error(apiEx.getCode(), apiEx.getMessage(), correlationId)));
                    }
                    log.error("{} failed | correlation_id={} | username={} | method={} | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                            method, correlationId, username, method, ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("SYSTEM_001", "Une erreur interne s'est produite", correlationId)));
                });
    }

    private String extractToken(String authHeader) {
        return authHeader.substring(7);
    }
}