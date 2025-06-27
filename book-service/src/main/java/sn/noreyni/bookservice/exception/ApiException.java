package sn.noreyni.bookservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception API LibroMesh
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public ApiException(HttpStatus status, String code, String message , Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    // Factory methods for common errors

    public static ApiException validationError(String field, String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_001",
                "Erreur de validation: " + message
        );
    }

    public static ApiException serviceUnavailable(String service) {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SERVICE_001",
                "Service temporairement indisponible, veuillez réessayer plus tard"
        );
    }

    public static ApiException internalError(String message) {
        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SYSTEM_001",
                "Une erreur interne s'est produite"
        );
    }

    public static ApiException badRequest(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "REQUEST_001",
                "Requête invalide"
        );
    }
}
