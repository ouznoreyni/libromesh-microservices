/*
package sn.noreyni.userservice.seed;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakSeeder implements CommandLineRunner {

    private final Keycloak keycloak;

    // Define the roles that need to be created
    private static final List<RoleData> LIBRARY_ROLES = Arrays.asList(
            new RoleData("SUPER_ADMIN", "Full system administrator with complete access"),
            new RoleData("LIBRARY_MANAGER", "Library manager with administrative privileges"),
            new RoleData("LIBRARIAN", "Professional librarian with full library services access"),
            new RoleData("CIRCULATION_STAFF", "Staff handling check-out/check-in operations"),
            new RoleData("CATALOGER", "Staff responsible for cataloging and metadata management"),
            new RoleData("REFERENCE_LIBRARIAN", "Specialist providing research and reference services"),
            new RoleData("ACQUISITIONS_LIBRARIAN", "Staff managing collection development and purchases"),
            new RoleData("SYSTEMS_ADMIN", "Technical administrator for library systems"),
            new RoleData("PATRON", "Regular library user with borrowing privileges"),
            new RoleData("GUEST", "Limited access user for basic services")
    );

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Keycloak roles seeding...");

        try {
            RealmResource realmResource = keycloak.realm("master"); // Change to your realm name
            RolesResource rolesResource = realmResource.roles();

            for (RoleData roleData : LIBRARY_ROLES) {
                createRoleIfNotExists(rolesResource, roleData);
            }

            log.info("Keycloak roles seeding completed successfully!");

        } catch (Exception e) {
            log.error("Error during Keycloak roles seeding: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void createRoleIfNotExists(RolesResource rolesResource, RoleData roleData) {
        try {
            // Check if role already exists
            RoleRepresentation existingRole = rolesResource.get(roleData.getName()).toRepresentation();
            log.info("Role '{}' already exists, skipping creation", roleData.getName());

        } catch (NotFoundException e) {
            // Role doesn't exist, create it
            try {
                RoleRepresentation roleRepresentation = new RoleRepresentation();
                roleRepresentation.setName(roleData.getName());
                roleRepresentation.setDescription(roleData.getDescription());
                roleRepresentation.setClientRole(false); // Realm-level role

                rolesResource.create(roleRepresentation);
                log.info("Successfully created role: '{}'", roleData.getName());

            } catch (Exception createException) {
                log.error("Failed to create role '{}': {}", roleData.getName(), createException.getMessage());
                throw createException;
            }
        } catch (Exception e) {
            log.error("Error checking role '{}': {}", roleData.getName(), e.getMessage());
            throw e;
        }
    }

    // Inner class to hold role data
    private static class RoleData {
        private final String name;
        private final String description;

        public RoleData(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}*/
