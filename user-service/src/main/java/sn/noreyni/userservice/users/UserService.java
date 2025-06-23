package sn.noreyni.userservice.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import sn.noreyni.userservice.common.PagedResponse;
import sn.noreyni.userservice.config.KeycloakAdminClientConfig;
import sn.noreyni.userservice.exception.ApiException;
import sn.noreyni.userservice.users.dto.CreateUserRequest;
import sn.noreyni.userservice.users.dto.CreateUserResponse;
import sn.noreyni.userservice.users.dto.UpdateUserRequest;
import sn.noreyni.userservice.users.dto.UserResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final Keycloak keycloak;
    private final KeycloakAdminClientConfig keycloakConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * Create a new user in Keycloak with optional roles
     */
    public Mono<CreateUserResponse> createUser(CreateUserRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User creation attempt started | correlation_id={} | username={} | method=createUser",
                correlationId, request.getUsername());

        return Mono.fromCallable(() -> {
            RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
            user.setEmailVerified(false);

            // Create user in Keycloak
            jakarta.ws.rs.core.Response response = realmResource.users().create(user);
            int status = response.getStatus();

            if (status >= 400) {
                String errorMessage = "Failed to create user in Keycloak, status: " + status;
                log.error("User creation failed | correlation_id={} | username={} | method=createUser | status=error | error_code=KEYCLOAK_ERROR | error_message={} | duration_ms={}",
                        correlationId, request.getUsername(), errorMessage, System.currentTimeMillis() - startTime);
                throw ApiException.badRequest(errorMessage);
            }

            // Extract user ID
            String userId = response.getLocation().getPath().substring(response.getLocation().getPath().lastIndexOf('/') + 1);

            // Set user password
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(request.getPassword());
            passwordCred.setTemporary(false);
            realmResource.users().get(userId).resetPassword(passwordCred);

            // Assign roles if provided
            if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                List<RoleRepresentation> rolesToAssign = request.getRoles().stream()
                        .map(roleName -> {
                            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                            if (role == null) {
                                throw ApiException.badRequest("Role not found: " + roleName);
                            }
                            return role;
                        })
                        .collect(Collectors.toList());
                realmResource.users().get(userId).roles().realmLevel().add(rolesToAssign);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("User creation successful | correlation_id={} | username={} | user_id={} | roles={} | method=createUser | status=success | duration_ms={}",
                    correlationId, request.getUsername(), userId, request.getRoles(), duration);

            return CreateUserResponse.builder()
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
        }).onErrorMap(ex -> {
            long duration = System.currentTimeMillis() - startTime;
            String errorCode = ex instanceof ApiException ? ((ApiException) ex).getCode() : "KEYCLOAK_ERROR";
            log.error("User creation failed | correlation_id={} | username={} | method=createUser | status=error | error_code={} | error_message={} | duration_ms={}",
                    correlationId, request.getUsername(), errorCode, ex.getMessage(), duration);
            return ex instanceof ApiException ? ex : ApiException.internalError("Failed to create user");
        });
    }

    /**
     * Get user details by ID
     */
    public Mono<UserResponse> getUser(String userId) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User retrieval attempt started | correlation_id={} | user_id={} | method=getUser",
                correlationId, userId);

        return Mono.fromCallable(() -> {
            RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            if (user == null) {
                throw ApiException.badRequest("User not found: " + userId);
            }

            List<String> roles = realmResource.users().get(userId).roles().realmLevel().listEffective()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.info("User retrieval successful | correlation_id={} | user_id={} | username={} | method=getUser | status=success | duration_ms={}",
                    correlationId, userId, user.getUsername(), duration);

            return UserResponse.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .enabled(user.isEnabled())
                    .emailVerified(user.isEmailVerified())
                    .createdAt(LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(user.getCreatedTimestamp()),
                            java.time.ZoneId.systemDefault()))
                    .roles(roles)
                    .build();
        }).onErrorMap(ex -> {
            long duration = System.currentTimeMillis() - startTime;
            String errorCode = ex instanceof ApiException ? ((ApiException) ex).getCode() : "KEYCLOAK_ERROR";
            log.error("User retrieval failed | correlation_id={} | user_id={} | method=getUser | status=error | error_code={} | error_message={} | duration_ms={}",
                    correlationId, userId, errorCode, ex.getMessage(), duration);
            return ex instanceof ApiException ? ex : ApiException.badRequest("User not found: " + userId);
        });
    }

    /**
     * List all users
     */
    public Mono<List<UserResponse>> listUsers() {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User list retrieval attempt started | correlation_id={} | method=listUsers",
                correlationId);

        return Mono.fromCallable(() -> {
            RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
            List<UserRepresentation> users = realmResource.users().list();

            List<UserResponse> userResponses = users.stream()
                    .map(user -> {
                        List<String> roles =
                                realmResource.users().get(user.getId()).roles().realmLevel().listAll()
                                        .stream()
                                        .map(RoleRepresentation::getName)
                                        .toList();
                        return UserResponse.builder()
                                .userId(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .enabled(user.isEnabled())
                                .emailVerified(user.isEmailVerified())
                                .createdAt(user.getCreatedTimestamp() != null ? LocalDateTime.ofInstant(
                                        Instant.ofEpochMilli(user.getCreatedTimestamp()),
                                        ZoneId.systemDefault()) : null)
                                .roles(roles)
                                .build();
                    })
                    .toList();

            long duration = System.currentTimeMillis() - startTime;
            log.info("User list retrieval successful | correlation_id={} | user_count={} | method=listUsers | status=success | duration_ms={}",
                    correlationId, userResponses.size(), duration);

            return userResponses;
        }).onErrorMap(ex -> {
            long duration = System.currentTimeMillis() - startTime;
            String errorCode = ex instanceof ApiException ? ((ApiException) ex).getCode() : "KEYCLOAK_ERROR";
            log.error("User list retrieval failed | correlation_id={} | method=listUsers | status=error | error_code={} | error_message={} | duration_ms={}",
                    correlationId, errorCode, ex.getMessage(), duration);
            return ex instanceof ApiException ? ex : ApiException.internalError("Failed to retrieve users");
        });
    }

    /**
     * List users with pagination
     */
    public Mono<PagedResponse<List<UserResponse>>> listUsers(int page, int size) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User list retrieval attempt started | correlation_id={} | page={} | size={} | method=listUsers",
                correlationId, page, size);

        return Mono.fromCallable(() -> {
                    RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());

                    // Validate pagination parameters
                    if (page < 0) {
                        throw ApiException.badRequest("Page number must be non-negative");
                    }
                    if (size < 1 || size > 100) {
                        throw ApiException.badRequest("Page size must be between 1 and 100");
                    }

                    // Calculate first result index
                    int first = page * size;

                    // Fetch paginated users
                    List<UserRepresentation> users = realmResource.users().list(first, size);

                    // Get total user count
                    long totalElements = realmResource.users().count();

                    // Map users to UserResponse
                    List<UserResponse> userResponses = users.stream()
                            .map(user -> {
                                List<String> roles =
                                        realmResource.users().get(user.getId()).roles().realmLevel().listAll()
                                        .stream()
                                        .map(RoleRepresentation::getName)
                                        .collect(Collectors.toList());

                                return UserResponse.builder()
                                        .userId(user.getId())
                                        .username(user.getUsername())
                                        .email(user.getEmail())
                                        .firstName(user.getFirstName())
                                        .lastName(user.getLastName())
                                        .enabled(user.isEnabled())
                                        .emailVerified(user.isEmailVerified())
                                        .createdAt(user.getCreatedTimestamp()!=null?
                                                LocalDateTime.ofInstant(
                                                java.time.Instant.ofEpochMilli(user.getCreatedTimestamp()),
                                                java.time.ZoneId.systemDefault()): null)
                                        .roles(roles)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    // Calculate total pages
                    int totalPages = (int) Math.ceil((double) totalElements / size);

                    long duration = System.currentTimeMillis() - startTime;
                    log.info("User list retrieval successful | correlation_id={} | page={} | size={} | user_count={} | total_elements={} | total_pages={} | method=listUsers | status=success | duration_ms={}",
                            correlationId, page, size, userResponses.size(), totalElements, totalPages, duration);

                    // Fix: Explicitly type the PagedResponse builder
                    return PagedResponse.<List<UserResponse>>builder()
                            .content(userResponses)
                            .totalElements(totalElements)
                            .totalPages(totalPages)
                            .currentPage(page)
                            .pageSize(size)
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic()) // Add scheduler for blocking operations
                .onErrorMap(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorCode = ex instanceof ApiException ? ((ApiException) ex).getCode() : "KEYCLOAK_ERROR";
                    log.error("User list retrieval failed | correlation_id={} | page={} | size={} | method=listUsers | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, page, size, errorCode, ex.getMessage(), duration);
                    return ex instanceof ApiException ? ex : ApiException.internalError("Failed to retrieve users");
                });
    }

    /**
     * Update user details and roles
     */
    public Mono<UserResponse> updateUser(String userId, UpdateUserRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User update attempt started | correlation_id={} | user_id={} | method=updateUser",
                correlationId, userId);

        return Mono.fromCallable(() -> {
            RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();
            if (user == null) {
                throw ApiException.badRequest("User not found: " + userId);
            }

            // Update user details if provided
            if (request.getEmail() != null) user.setEmail(request.getEmail());
            if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
            if (request.getLastName() != null) user.setLastName(request.getLastName());
            if (request.getEnabled() != null) user.setEnabled(request.getEnabled());

            realmResource.users().get(userId).update(user);

            // Update roles if provided
            if (request.getRoles() != null) {
                // Remove existing realm roles
                List<RoleRepresentation> currentRoles = realmResource.users().get(userId).roles().realmLevel().listEffective();
                realmResource.users().get(userId).roles().realmLevel().remove(currentRoles);

                // Add new roles
                List<RoleRepresentation> rolesToAssign = request.getRoles().stream()
                        .map(roleName -> {
                            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                            if (role == null) {
                                throw ApiException.badRequest("Role not found: " + roleName);
                            }
                            return role;
                        })
                        .collect(Collectors.toList());
                realmResource.users().get(userId).roles().realmLevel().add(rolesToAssign);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("User update successful | correlation_id={} | user_id={} | username={} | method=updateUser | status=success | duration_ms={}",
                    correlationId, userId, user.getUsername(), duration);

            List<String> updatedRoles = realmResource.users().get(userId).roles().realmLevel().listEffective()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());

            return UserResponse.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .enabled(user.isEnabled())
                    .emailVerified(user.isEmailVerified())
                    .createdAt(LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(user.getCreatedTimestamp()),
                            java.time.ZoneId.systemDefault()))
                    .roles(updatedRoles)
                    .build();
        }).onErrorMap(ex -> {
            long duration = System.currentTimeMillis() - startTime;
            String errorCode = ex instanceof ApiException ? ((ApiException) ex).getCode() : "KEYCLOAK_ERROR";
            log.error("User update failed | correlation_id={} | user_id={} | method=updateUser | status=error | error_code={} | error_message={} | duration_ms={}",
                    correlationId, userId, errorCode, ex.getMessage(), duration);
            return ex instanceof ApiException ? ex : ApiException.badRequest("User not found: " + userId);
        });
    }

    /**
     * Delete a user by ID
     */
    public Mono<Void> deleteUser(String userId) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User deletion attempt started | correlation_id={} | user_id={} | method=deleteUser",
                correlationId, userId);

        return Mono.fromCallable(() -> {
            RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
            jakarta.ws.rs.core.Response response = realmResource.users().delete(userId);
            int status = response.getStatus();

            if (status >= 400) {
                String errorMessage = "Failed to delete user in Keycloak, status: " + status;
                log.error("User deletion failed | correlation_id={} | user_id={} | method=deleteUser | status=error | error_code=KEYCLOAK_ERROR | error_message={} | duration_ms={}",
                        correlationId, userId, errorMessage, System.currentTimeMillis() - startTime);
                throw ApiException.badRequest("User not found: " + userId);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("User deletion successful | correlation_id={} | user_id={} | method=deleteUser | status=success | duration_ms={}",
                    correlationId, userId, duration);
            return null;
        }).then().onErrorMap(ex -> {
            long duration = System.currentTimeMillis() - startTime;
            String errorCode = ex instanceof ApiException ? ((ApiException) ex).getCode() : "KEYCLOAK_ERROR";
            log.error("User deletion failed | correlation_id={} | user_id={} | method=deleteUser | status=error | error_code={} | error_message={} | duration_ms={}",
                    correlationId, userId, errorCode, ex.getMessage(), duration);
            return ex instanceof ApiException ? ex : ApiException.badRequest("User not found: " + userId);
        });
    }

}