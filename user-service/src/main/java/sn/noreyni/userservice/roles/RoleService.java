package sn.noreyni.userservice.roles;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import sn.noreyni.userservice.common.ApiResponse;
import sn.noreyni.userservice.common.PagedResponse;
import sn.noreyni.userservice.config.KeycloakAdminClientConfig;
import sn.noreyni.userservice.exception.ApiException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final Keycloak keycloak;
    private final KeycloakAdminClientConfig keycloakConfig;

    /**
     * List all roles
     */
    public Mono<List<RoleDTO>> listAllRoles() {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Role list retrieval attempt started | correlation_id={} | method=listAllRoles", correlationId);

        return Mono.fromCallable(() -> fetchAllRoles(correlationId, startTime))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ex -> handleError(ex, correlationId, startTime, "listAllRoles"));
    }

    /**
     * List roles with pagination
     */
    public Mono<ApiResponse<List<RoleDTO>>> rolesPaged(int page, int size) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Paged role list retrieval started | correlation_id={} | page={} | size={} | method=listRolesPaged",
                correlationId, page, size);

        return Mono.fromCallable(() -> fetchPagedRoles(page, size, correlationId, startTime))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ex -> handleError(ex, correlationId, startTime, "listRolesPaged"));
    }

    private List<RoleDTO> fetchAllRoles(String correlationId, long startTime) {
        List<RoleRepresentation> roles = keycloak.realm(keycloakConfig.getRealm()).roles().list();
        List<RoleDTO> roleDTOs = roles.stream().map(this::toRoleDTO).toList();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Role list retrieval successful | correlation_id={} | role_count={} | method=listAllRoles | status=success | duration_ms={}",
                correlationId, roleDTOs.size(), duration);

        return roleDTOs;
    }

    private ApiResponse<List<RoleDTO>> fetchPagedRoles(int page, int size, String correlationId,
                                                       long startTime) {
        validatePaginationParameters(page, size);

        List<RoleRepresentation> roles = keycloak.realm(keycloakConfig.getRealm()).roles().list(page * size, size);
        long totalElements = keycloak.realm(keycloakConfig.getRealm()).roles().list().size();
        List<RoleDTO> roleDTOs = roles.stream().map(this::toRoleDTO).toList();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Paged role list retrieval successful | correlation_id={} | page={} | size={} | role_count={} | total_elements={} | total_pages={} | method=listRolesPaged | status=success | duration_ms={}",
                correlationId, page, size, roleDTOs.size(), totalElements, totalPages, duration);

        return ApiResponse.<List<RoleDTO>>builder()
                .success(true)
                .message("Role list retrieval successful")
                .data(roleDTOs)
                .pagination(ApiResponse.Pagination
                        .builder()
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .currentPage(page)
                        .pageSize(size)
                        .build())
                .build();
    }

    private RoleDTO toRoleDTO(RoleRepresentation role) {
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .composite(role.isComposite())
                .clientRole(role.getClientRole())
                .containerId(role.getContainerId())
                .build();
    }

    private void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw ApiException.badRequest("Le numéro de page doit être positif ou nul.");
        }
        if (size < 1 || size > 100) {
            throw ApiException.badRequest("La taille de page doit être comprise entre 1 et 100.");
        }
    }

    private Throwable handleError(Throwable ex, String correlationId, long startTime, String method) {
        long duration = System.currentTimeMillis() - startTime;
        String errorCode = ex instanceof ApiException apiException ? apiException.getCode() : "KEYCLOAK_ERROR";
        String errorMessage = ex.getMessage();
        log.error("{} failed | correlation_id={} | method={} | status=error | error_code={} | error_message={} | duration_ms={}",
                method, correlationId, method, errorCode, errorMessage, duration);
        return ex instanceof ApiException ? ex : ApiException.internalError("Échec lors du traitement de la requête des rôles.");
    }

}