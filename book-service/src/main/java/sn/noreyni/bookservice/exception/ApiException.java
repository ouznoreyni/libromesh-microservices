package sn.noreyni.userservice.exception;

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

    public ApiException(HttpStatus status, String code, String message ,Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    // Factory methods for common errors
    public static ApiException authenticationFailed() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "AUTH_001",
                "Nom d'utilisateur ou mot de passe incorrect"
        );
    }

    public static ApiException userNotFound(String username) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "USER_001",
                "Utilisateur introuvable"
        );
    }

    public static ApiException tokenExpired() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "AUTH_002",
                "Votre session a expiré, veuillez vous reconnecter"
        );
    }

    public static ApiException invalidToken() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "AUTH_003",
                "Token invalide"
        );
    }

    public static ApiException accessDenied() {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "AUTH_004",
                "Accès refusé - permissions insuffisantes"
        );
    }

    public static ApiException userAlreadyExists(String username) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "USER_002",
                "Un utilisateur avec ce nom existe déjà"
        );
    }

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
