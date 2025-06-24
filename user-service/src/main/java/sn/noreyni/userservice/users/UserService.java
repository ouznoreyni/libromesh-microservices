package sn.noreyni.userservice.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import sn.noreyni.userservice.common.ApiResponse;
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

import jakarta.ws.rs.core.Response;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final Keycloak keycloak;
    private final KeycloakAdminClientConfig keycloakConfig;

    /**
     * Create a new user in Keycloak with optional roles
     */
    public Mono<CreateUserResponse> createUser(CreateUserRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User creation attempt started | correlation_id={} | username={} | method=createUser",
                correlationId, request.getUsername());

        return Mono.fromCallable(() -> createUserInKeycloak(request, correlationId, startTime))
                .onErrorMap(ex -> handleError(ex, correlationId, startTime, request.getUsername(), "createUser"));
    }

    private CreateUserResponse createUserInKeycloak(CreateUserRequest request, String correlationId, long startTime) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        UserRepresentation user = buildUserRepresentation(request);

        // Create user
        Response response = realmResource.users().create(user);
        validateResponse(response, correlationId, request.getUsername(), startTime);

        // Extract user ID
        String userId = extractUserId(response);

        // Set password
        setUserPassword(realmResource, userId, request.getPassword());

        // Assign roles
        assignRolesIfProvided(realmResource, userId, request.getRoles());

        long duration = System.currentTimeMillis() - startTime;
        log.info("User creation successful | correlation_id={} | username={} | user_id={} | roles={} | method=createUser | status=success | duration_ms={}",
                correlationId, request.getUsername(), userId, request.getRoles(), duration);

        return CreateUserResponse.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserRepresentation buildUserRepresentation(CreateUserRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(request.getEnabled());
        user.setEmailVerified(false);
        return user;
    }

    private void validateResponse(Response response, String correlationId, String username, long startTime) {
        int status = response.getStatus();
        if (status >= 400) {
            String errorMessage = "Failed to create user in Keycloak, status: " + status;
            log.error("User creation failed | correlation_id={} | username={} | method=createUser | status=error | error_code=KEYCLOAK_ERROR | error_message={} | duration_ms={}",
                    correlationId, username, errorMessage, System.currentTimeMillis() - startTime);
            throw ApiException.badRequest(errorMessage);
        }
    }

    private String extractUserId(Response response) {
        return response.getLocation().getPath().substring(response.getLocation().getPath().lastIndexOf('/') + 1);
    }

    private void setUserPassword(RealmResource realmResource, String userId, String password) {
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(password);
        passwordCred.setTemporary(false);
        realmResource.users().get(userId).resetPassword(passwordCred);
    }

    private void assignRolesIfProvided(RealmResource realmResource, String userId, List<String> roles) {
        if (roles != null && !roles.isEmpty()) {
            assignRolesToUser(realmResource, userId, roles);
        }
    }

    /**
     * Get user details by ID
     */
    public Mono<UserResponse> getUser(String userId) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User retrieval attempt started | correlation_id={} | user_id={} | method=getUser",
                correlationId, userId);

        return Mono.fromCallable(() -> fetchUserDetails(userId, correlationId, startTime))
                .onErrorMap(ex -> handleError(ex, correlationId, startTime, userId, "getUser"));
    }

    private UserResponse fetchUserDetails(String userId, String correlationId, long startTime) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        UserRepresentation user = getUserRepresentation(realmResource, userId);
        List<String> roles = getUserRoles(realmResource, userId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("User retrieval successful | correlation_id={} | user_id={} | username={} | method=getUser | status=success | duration_ms={}",
                correlationId, userId, user.getUsername(), duration);

        return buildUserResponse(user, roles);
    }

    private UserRepresentation getUserRepresentation(RealmResource realmResource, String userId) {
        UserRepresentation user = realmResource.users().get(userId).toRepresentation();
        if (user == null) {
            throw ApiException.badRequest("User not found: " + userId);
        }
        return user;
    }

    private List<String> getUserRoles(RealmResource realmResource, String userId) {
        return realmResource.users().get(userId).roles().realmLevel().listAll()
                .stream()
                .map(RoleRepresentation::getName)
                .toList();
    }

    private UserResponse buildUserResponse(UserRepresentation user, List<String> roles) {
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
    }

    /**
     * List all users
     */
    public Mono<List<UserResponse>> listUsers() {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User list retrieval attempt started | correlation_id={} | method=listUsers",
                correlationId);

        return Mono.fromCallable(() -> fetchAllUsers(correlationId, startTime))
                .onErrorMap(ex -> handleError(ex, correlationId, startTime, null, "listUsers"));
    }

    private List<UserResponse> fetchAllUsers(String correlationId, long startTime) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        List<UserRepresentation> users = realmResource.users().list();

        List<UserResponse> userResponses = users.stream()
                .map(user -> buildUserResponse(user, getUserRoles(realmResource, user.getId())))
                .toList();

        long duration = System.currentTimeMillis() - startTime;
        log.info("User list retrieval successful | correlation_id={} | user_count={} | method=listUsers | status=success | duration_ms={}",
                correlationId, userResponses.size(), duration);

        return userResponses;
    }

    /**
     * List users with pagination
     */
    public Mono<ApiResponse<List<UserResponse>>> listUsers(int page, int size) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User list retrieval attempt started | correlation_id={} | page={} | size={} | method=listUsers",
                correlationId, page, size);

        return Mono.fromCallable(() -> fetchPagedUsers(page, size, correlationId, startTime))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ex -> handleError(ex, correlationId, startTime, null, "listUsers"));
    }

    private ApiResponse<List<UserResponse>> fetchPagedUsers(int page, int size, String correlationId, long startTime) {
        validatePaginationParameters(page, size);

        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        int first = page * size;
        List<UserRepresentation> users = realmResource.users().list(first, size);
        long totalElements = realmResource.users().count();

        List<UserResponse> userResponses = users.stream()
                .map(user -> buildUserResponse(user, getUserRoles(realmResource, user.getId())))
                .toList();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        long duration = System.currentTimeMillis() - startTime;
        log.info("User list retrieval successful | correlation_id={} | page={} | size={} | user_count={} | total_elements={} | total_pages={} | method=listUsers | status=success | duration_ms={}",
                correlationId, page, size, userResponses.size(), totalElements, totalPages, duration);

        return ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .message("User list retrieval successful")
                .data(userResponses)
                .pagination(ApiResponse.Pagination
                        .builder()
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .currentPage(page)
                        .pageSize(size)
                        .build())
                .build();
    }

    private void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw ApiException.badRequest("Page number must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw ApiException.badRequest("Page size must be between 1 and 100");
        }
    }

    /**
     * Update user details and roles
     */
    public Mono<UserResponse> updateUser(String userId, UpdateUserRequest request) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("User update attempt started | correlation_id={} | user_id={} | method=updateUser",
                correlationId, userId);

        return Mono.fromCallable(() -> updateUserDetails(userId, request, correlationId, startTime))
                .onErrorMap(ex -> handleError(ex, correlationId, startTime, userId, "updateUser"));
    }

    private UserResponse updateUserDetails(String userId, UpdateUserRequest request, String correlationId, long startTime) {
        RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
        UserRepresentation user = getUserRepresentation(realmResource, userId);

        updateUserAttributes(user, request);
        realmResource.users().get(userId).update(user);

        updateUserRolesIfProvided(realmResource, userId, request.getRoles());

        long duration = System.currentTimeMillis() - startTime;
        log.info("User update successful | correlation_id={} | user_id={} | username={} | method=updateUser | status=success | duration_ms={}",
                correlationId, userId, user.getUsername(), duration);

        return buildUserResponse(user, getUserRoles(realmResource, userId));
    }

    private void updateUserAttributes(UserRepresentation user, UpdateUserRequest request) {
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());
    }

    private void updateUserRolesIfProvided(RealmResource realmResource, String userId, List<String> roles) {
        if (roles != null) {
            List<RoleRepresentation> currentRoles = realmResource.users().get(userId).roles().realmLevel().listEffective();
            realmResource.users().get(userId).roles().realmLevel().remove(currentRoles);
            assignRolesToUser(realmResource, userId, roles);
        }
    }

    private void assignRolesToUser(RealmResource realmResource, String userId, List<String> roles) {
        List<RoleRepresentation> rolesToAssign = roles.stream()
                .map(roleName -> {
                    RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                    if (role == null) {
                        throw ApiException.badRequest("Role not found: " + roleName);
                    }
                    return role;
                })
                .toList();
        realmResource.users().get(userId).roles().realmLevel().add(rolesToAssign);
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
                    Response response = realmResource.users().delete(userId);
                    validateDeleteResponse(response, userId, correlationId, startTime);
                    return null;
                })
                .then()
                .onErrorMap(ex -> handleError(ex, correlationId, startTime, userId, "getUser"));
    }


    private void validateDeleteResponse(Response response, String userId, String correlationId, long startTime) {
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
    }

    private Throwable handleError(Throwable ex, String correlationId, long startTime, String identifier, String method) {
        long duration = System.currentTimeMillis() - startTime;
        String errorCode = ex instanceof ApiException apiException ? apiException.getCode() : "KEYCLOAK_ERROR";
        String errorMessage = ex.getMessage();
        log.error("{} failed | correlation_id={} | {} | method={} | status=error | error_code={} | error_message={} | duration_ms={}",
                method, correlationId, identifier != null ? "identifier=" + identifier : "", method, errorCode, errorMessage, duration);
        return ex instanceof ApiException ? ex : ApiException.internalError("Failed to process request");
    }
}