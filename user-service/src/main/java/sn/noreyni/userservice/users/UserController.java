package sn.noreyni.userservice.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sn.noreyni.userservice.common.ApiResponse;
import sn.noreyni.userservice.common.PagedResponse;
import sn.noreyni.userservice.exception.ApiException;
import sn.noreyni.userservice.users.dto.CreateUserRequest;
import sn.noreyni.userservice.users.dto.CreateUserResponse;
import sn.noreyni.userservice.users.dto.UpdateUserRequest;
import sn.noreyni.userservice.users.dto.UserResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    /**
     * Create a new user (admin action)
     */
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<CreateUserResponse>>> createUser(@Valid @RequestBody CreateUserRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User creation request started | correlation_id={} | username={} | method=createUser", correlationId, request.getUsername());

        return userService.createUser(request)
                .map(createUserResponse -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User creation successful | correlation_id={} | username={} | user_id={} | method=createUser | status=success | duration_ms={}",
                            correlationId, request.getUsername(), createUserResponse.getUserId(), duration);
                    return ResponseEntity.ok(
                            ApiResponse.<CreateUserResponse>success(
                                    createUserResponse,
                                    "Utilisateur créé avec succès",
                                    correlationId
                            ).withMetadata("created_at", createUserResponse.getCreatedAt().toString())
                    );
                })
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("User creation failed | correlation_id={} | username={} | method=createUser | status=error | error_code={} | error_message={} | duration_ms={}",
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
                    log.error("Unexpected error during user creation | correlation_id={} | username={} | method=createUser | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
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
     * Get user details by ID
     */
    @GetMapping("/{userId}")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> getUser(@PathVariable String userId) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User retrieval request started | correlation_id={} | user_id={} | method=getUser", correlationId, userId);

        return userService.getUser(userId)
                .map(userResponse -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User retrieval successful | correlation_id={} | user_id={} | username={} | method=getUser | status=success | duration_ms={}",
                            correlationId, userId, userResponse.getUsername(), duration);
                    return ResponseEntity.ok(
                            ApiResponse.<UserResponse>success(
                                    userResponse,
                                    "Utilisateur récupéré avec succès",
                                    correlationId
                            ));
                })
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("User retrieval failed | correlation_id={} | user_id={} | method=getUser | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, userId, ex.getCode(), ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(ex.getStatus())
                            .body(ApiResponse.error(
                                    ex.getCode(),
                                    ex.getMessage(),
                                    correlationId
                            )));
                })
                .onErrorResume(Exception.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Unexpected error during user retrieval | correlation_id={} | user_id={} | method=getUser | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                            correlationId, userId, ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(
                                    "SYSTEM_001",
                                    "Une erreur interne s'est produite",
                                    correlationId
                            )));
                });
    }

    /**
     * List all users
     */
    @GetMapping("/all")
    public Mono<ResponseEntity<ApiResponse<List<UserResponse>>>> listUsers() {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User list retrieval request started | correlation_id={} | method=listUsers", correlationId);

        return userService.listUsers()
                .map(userResponses -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User list retrieval successful | correlation_id={} | user_count={} | method=listUsers | status=success | duration_ms={}",
                            correlationId, userResponses.size(), duration);
                    return ResponseEntity.ok(
                            ApiResponse.<List<UserResponse>>success(
                                    userResponses,
                                    "Liste des utilisateurs récupérée avec succès",
                                    correlationId
                            ));
                })
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("User list retrieval failed | correlation_id={} | method=listUsers | status=error | error_code={} | error_message={} | duration_ms={}",
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
                    log.error("Unexpected error during user list retrieval | correlation_id={} | method=listUsers | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
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
     * List all users with pagination
     */
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<PagedResponse<List<UserResponse>>>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User list retrieval request started | correlation_id={} | page={} | size={} | method=listUsers", correlationId, page, size);

        return userService.listUsers(page, size)
                .map(pagedUserResponse -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User list retrieval successful | correlation_id={} | page={} | size={} | user_count={} | total_elements={} | total_pages={} | method=listUsers | status=success | duration_ms={}",
                            correlationId, page, size, pagedUserResponse.getContent().size(), pagedUserResponse.getTotalElements(), pagedUserResponse.getTotalPages(), duration);
                    return ResponseEntity.ok(
                            ApiResponse.<PagedResponse<List<UserResponse>>>success(
                                    pagedUserResponse,
                                    "Liste des utilisateurs récupérée avec succès",
                                    correlationId
                            )
                    );
                })
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("User list retrieval failed | correlation_id={} | page={} | size={} | method=listUsers | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, page, size, ex.getCode(), ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(ex.getStatus())
                            .body(ApiResponse.error(
                                    ex.getCode(),
                                    ex.getMessage(),
                                    correlationId
                            )));
                })
                .onErrorResume(Exception.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Unexpected error during user list retrieval | correlation_id={} | page={} | size={} | method=listUsers | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                            correlationId, page, size, ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(
                                    "SYSTEM_001",
                                    "Une erreur interne s'est produite",
                                    correlationId
                            )));
                });
    }

    /**
     * Update user details and roles
     */
    @PutMapping("/{userId}")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> updateUser(
            @PathVariable String userId, @Valid @RequestBody UpdateUserRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User update request started | correlation_id={} | user_id={} | method=updateUser", correlationId, userId);

        return userService.updateUser(userId, request)
                .map(userResponse -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User update successful | correlation_id={} | user_id={} | username={} | method=updateUser | status=success | duration_ms={}",
                            correlationId, userId, userResponse.getUsername(), duration);
                    return ResponseEntity.ok(
                            ApiResponse.<UserResponse>success(
                                    userResponse,
                                    "Utilisateur mis à jour avec succès",
                                    correlationId
                            ));
                })
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("User update failed | correlation_id={} | user_id={} | method=updateUser | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, userId, ex.getCode(), ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(ex.getStatus())
                            .body(ApiResponse.error(
                                    ex.getCode(),
                                    ex.getMessage(),
                                    correlationId
                            )));
                })
                .onErrorResume(Exception.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Unexpected error during user update | correlation_id={} | user_id={} | method=updateUser | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                            correlationId, userId, ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(
                                    "SYSTEM_001",
                                    "Une erreur interne s'est produite",
                                    correlationId
                            )));
                });
    }

    /**
     * Delete a user by ID
     */
    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteUser(@PathVariable String userId) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User deletion request started | correlation_id={} | user_id={} | method=deleteUser", correlationId, userId);

        return userService.deleteUser(userId)
                .then(Mono.fromCallable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User deletion successful | correlation_id={} | user_id={} | method=deleteUser | status=success | duration_ms={}",
                            correlationId, userId, duration);
                    return ResponseEntity.ok(
                            ApiResponse.<Void>success(
                                    null,
                                    "Utilisateur supprimé avec succès",
                                    correlationId
                            ));
                }))
                .onErrorResume(ApiException.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("User deletion failed | correlation_id={} | user_id={} | method=deleteUser | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, userId, ex.getCode(), ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(ex.getStatus())
                            .body(ApiResponse.error(
                                    ex.getCode(),
                                    ex.getMessage(),
                                    correlationId
                            )));
                })
                .onErrorResume(Exception.class, ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Unexpected error during user deletion | correlation_id={} | user_id={} | method=deleteUser | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                            correlationId, userId, ex.getMessage(), duration);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(
                                    "SYSTEM_001",
                                    "Une erreur interne s'est produite",
                                    correlationId
                            )));
                });
    }
}
