package sn.noreyni.userservice.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserRequest {

    @Email(message = "Format d'email invalide")
    private String email;

    private String firstName;
    private String lastName;
    private Boolean enabled;
    private List<String> roles;
}