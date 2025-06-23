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

import java.time.LocalDateTime;
import java.util.List;
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
                .map(response -> buildSuccessResponse(response, "Utilisateur créé avec succès", correlationId, startTime))
                .onErrorResume(ex -> handleError(ex, correlationId, startTime, request.getUsername(), "createUser"));
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
                .map(response -> buildSuccessResponse(response, "Utilisateur récupéré avec succès", correlationId, startTime))
                .onErrorResume(ex -> handleError(ex, correlationId, startTime, userId, "getUser"));
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
                .map(response -> buildSuccessResponse(response, "Liste des utilisateurs récupérée avec succès", correlationId, startTime))
                .onErrorResume(ex -> handleError(ex, correlationId, startTime, null, "listUsers"));
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
                .map(response -> buildSuccessResponse(response, "Liste des utilisateurs récupérée avec succès", correlationId, startTime))
                .onErrorResume(ex -> handleError(ex, correlationId, startTime, null, "listUsers"));
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
                .map(response -> buildSuccessResponse(response, "Utilisateur mis à jour avec succès", correlationId, startTime))
                .onErrorResume(ex -> handleError(ex, correlationId, startTime, userId, "updateUser"));
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
                .then(Mono.just(buildSuccessResponse((Void) null, "Utilisateur supprimé avec succès", correlationId, startTime)))
                .onErrorResume(ex -> handleError(ex, correlationId, startTime, userId, "deleteUser"));
    }

    private <T> ResponseEntity<ApiResponse<T>> buildSuccessResponse(T response, String message, String correlationId, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Request successful | correlation_id={} | method={} | status=success | duration_ms={}",
                correlationId, message, duration);
        return ResponseEntity.ok(
                ApiResponse.<T>success(response, message, correlationId)
                        .withMetadata("created_at", LocalDateTime.now().toString()));
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> handleError(Throwable ex, String correlationId, long startTime, String identifier, String method) {
        long duration = System.currentTimeMillis() - startTime;
        if (ex instanceof ApiException apiEx) {
            log.error("{} failed | correlation_id={} | {} | method={} | status=error | error_code={} | error_message={} | duration_ms={}",
                    method, correlationId, identifier != null ? "identifier=" + identifier : "", method, apiEx.getCode(), ex.getMessage(), duration);
            return Mono.just(ResponseEntity.status(apiEx.getStatus())
                    .body(ApiResponse.error(apiEx.getCode(), ex.getMessage(), correlationId)));
        }
        log.error("Unexpected error during {} | correlation_id={} | {} | method={} | status=error | error_code=SYSTEM_001 | error_message={} | duration_ms={}",
                method, correlationId, identifier != null ? "identifier=" + identifier : "", method, ex.getMessage(), duration);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("SYSTEM_001", "Une erreur interne s'est produite", correlationId)));
    }
}