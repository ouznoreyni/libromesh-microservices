package sn.noreyni.userservice.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    @NotBlank(message = "Le jeton de rafraîchissement est obligatoire")
    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("logout_all_devices")
    private Boolean logoutAllDevices = false;
}