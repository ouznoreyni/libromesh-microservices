package sn.noreyni.userservice.roles;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import sn.noreyni.userservice.common.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/all")
    public Mono<ApiResponse<List<RoleDTO>>> listAllRoles() {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Role list retrieval attempt started | correlation_id={} | method=listAllRoles", correlationId);

        return roleService.listAllRoles()
                .map(roles -> {
                    ApiResponse<List<RoleDTO>> response = new ApiResponse<>();
                    response.setSuccess(true);
                    response.setData(roles);
                    response.setMessage("Paged role list retrieved successfully");
                    return response;
                })
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Role list retrieval successful | correlation_id={} | role_count={} | method=listAllRoles | status=success | duration_ms={}",
                            correlationId, response.getData().size(), duration);
                })
                .doOnError(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorCode = ex instanceof sn.noreyni.userservice.exception.ApiException apiException ? apiException.getCode() : "INTERNAL_ERROR";
                    log.error("Role list retrieval failed | correlation_id={} | method=listAllRoles | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, errorCode, ex.getMessage(), duration);
                });
    }

    @GetMapping
    public Mono<ApiResponse<List<RoleDTO>>> listRolesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        log.info("Paged role list retrieval attempt started | correlation_id={} | page={} | size={} | method=listRolesPaged",
                correlationId, page, size);

        return roleService.rolesPaged(page, size)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Paged role list retrieval successful | correlation_id={} | page={} | size={} | role_count={} | total_elements={} | total_pages={} | method=listRolesPaged | status=success | duration_ms={}",
                            correlationId, page, size, response.getData().size(),
                            response.getPagination().getTotalElements(), response.getPagination().getTotalPages(), duration);
                })
                .doOnError(ex -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorCode = ex instanceof sn.noreyni.userservice.exception.ApiException apiException ? apiException.getCode() : "INTERNAL_ERROR";
                    log.error("Paged role list retrieval failed | correlation_id={} | page={} | size={} | method=listRolesPaged | status=error | error_code={} | error_message={} | duration_ms={}",
                            correlationId, page, size, errorCode, ex.getMessage(), duration);
                });
    }
}
