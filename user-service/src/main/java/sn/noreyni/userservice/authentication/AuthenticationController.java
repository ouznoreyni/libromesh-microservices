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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> login(@Valid @RequestBody LoginRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Login request started | correlation_id={} | username={} | method=login", correlationId, request.getUsername());

        return authenticationService.login(request)
                .map(loginResponse -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Login successful | correlation_id={} | username={} | method=login | status=success | duration_ms={}",
                            correlationId, request.getUsername(), duration);
                    return ResponseEntity.ok(
                            ApiResponse.<LoginResponse>success(
                                    loginResponse,
                                    "Connexion réussie",
                                    correlationId
                            ).withMetadata("login_time", loginResponse.getLoginTime().toString())
                    );
                })
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Login failed | correlation_id={} | username={} | method=login | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, request.getUsername(), ex.getCode(), ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(ex.getStatus())
                            .body(ApiResponse.error(
                                    ex.getCode(),
                                    ex.getMessage(),
                                    correlationId
                            )));
                })
                .onErrorResume(Exception.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Unexpected error during login | correlation_id={} | username={} | method=login | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                            correlationId, request.getUsername(), ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(
                                    "SYSTEM_001",
                                    "Une erreur interne s'est produite",
                                    correlationId
                            )));
                });
    }

    /**
     * Token refresh endpoint
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<RefreshTokenResponse>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Token refresh request started | correlation_id={} | method=refreshToken", correlationId);

        return authenticationService.refreshToken(request)
                .map(refreshResponse -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Token refresh successful | correlation_id={} | method=refreshToken | status=success | duration_ms={}",
                            correlationId, duration);
                    return ResponseEntity.ok(
                            ApiResponse.<RefreshTokenResponse>success(
                                    refreshResponse,
                                    "Jeton rafraîchi avec succès",
                                    correlationId
                            ).withMetadata("refreshed_at", refreshResponse.getRefreshedAt().toString())
                    );
                })
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Token refresh failed | correlation_id={} | method=refreshToken | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, ex.getCode(), ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(ex.getStatus())
                            .body(ApiResponse.error(
                                    ex.getCode(),
                                    ex.getMessage(),
                                    correlationId
                            )));
                })
                .onErrorResume(Exception.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Unexpected error during token refresh | correlation_id={} | method=refreshToken | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                            correlationId, ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(
                                    "SYSTEM_001",
                                    "Une erreur interne s'est produite",
                                    correlationId
                            )));
                });
    }

    /**
     * User logout endpoint
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(@Valid @RequestBody LogoutRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Logout request started | correlation_id={} | method=logout", correlationId);

        return authenticationService.logout(request)
                .then(Mono.fromCallable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Logout successful | correlation_id={} | method=logout | status=success | duration_ms={}",
                            correlationId, duration);
                    return ResponseEntity.ok(
                            ApiResponse.<Void>success(
                                    null,
                                    "Déconnexion réussie",
                                    correlationId
                            ).withMetadata("logout_time", java.time.LocalDateTime.now().toString())
                    );
                }))
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Logout failed | correlation_id={} | method=logout | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, ex.getCode(), ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(ex.getStatus())
                            .body(ApiResponse.<Void>error(
                                    ex.getCode(),
                                    ex.getMessage(),
                                    correlationId
                            )));
                })
                .onErrorResume(Exception.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Unexpected error during logout | correlation_id={} | method=logout | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                            correlationId, ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.<Void>error(
                                    "SYSTEM_001",
                                    "Une erreur interne s'est produite",
                                    correlationId
                            )));
                });
    }



    /**
     * Get current user information from token -
     */

    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<UserInfo>>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            "AUTH_001",
                            "En-tête d'autorisation manquant ou invalide",
                            "/api/v1/auth/me"
                    )));
        }

        String token = authHeader.substring(7);
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Current user info request started | correlation_id={} | method=getCurrentUser", correlationId);

        return authenticationService.getUserInformationFromToken(token)
                .map(userInfo -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User information retrieved | correlation_id={} | username={} | method=getCurrentUser | status=success | duration_ms={}",
                            correlationId, userInfo.getUsername(), duration);
                    return ResponseEntity.ok(
                            ApiResponse.success(
                                    userInfo,
                                    "Informations utilisateur récupérées",
                                    correlationId
                            ));
                })
                .onErrorResume(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("User information retrieval failed | correlation_id={} | method=getCurrentUser | status=error | error_code=AUTH_003 | error_message={} | duration_ms={}",
                            correlationId, ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error(
                                    "AUTH_003",
                                    "Jeton invalide ou expiré",
                                    correlationId
                            )));
                });
    }

}