package sn.noreyni.bookservice.common;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Pagination pagination;
    private ErrorDetails error;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String code;
        private String message;
        private Object details;
        private Map<String, String> validationErrors;
    }

    // Factory methods
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Opération réussie")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Factory methods
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message("Erreur lors de l'opération")
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, Object details) {
        return ApiResponse.<T>builder()
                .success(false)
                .message("Erreur lors de l'opération")
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> validationError(Map<String, String> validationErrors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message("Erreur de validation")
                .error(ErrorDetails.builder()
                        .code("VALIDATION_001")
                        .message("Les données saisies ne sont pas valides")
                        .validationErrors(validationErrors)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public boolean hasError() {
        return !success && error != null;
    }

    public boolean hasData() {
        return data != null;
    }

    public ApiResponse<T> withPath(String path) {
        return this;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Pagination {
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
    }

}