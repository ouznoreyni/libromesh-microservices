package sn.noreyni.userservice.roles;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RoleDTO {
    String id;
    String name;
    String description;
    @JsonProperty("composite")
    Boolean composite;
    @JsonProperty("clientRole")
    Boolean clientRole;
    @JsonProperty("containerId")
    String containerId;
}
