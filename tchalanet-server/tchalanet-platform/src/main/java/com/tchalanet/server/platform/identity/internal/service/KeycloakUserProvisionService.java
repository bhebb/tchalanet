package com.tchalanet.server.platform.identity.internal.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

/**
 * Creates users in Keycloak via the admin API.
 * Only active when {@code kc.bootstrap.enabled=true}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserProvisionService {

  private final KeycloakBootstrapProperties props;

  /**
   * Creates a Keycloak user with the given attributes and realm role.
   * Returns the Keycloak UUID of the created (or already existing) user.
   * If bootstrap is disabled, returns an empty result.
   */
  public KeycloakProvisionResult provisionUser(
      String username,
      String email,
      String firstName,
      String lastName,
      String tenantCode,
      String plan,
      String realmRole,
      String tempPassword) {

    if (!props.enabled()) {
      log.warn("KC bootstrap disabled — skipping Keycloak user creation for {}", username);
      return new KeycloakProvisionResult(null, false);
    }

    var adminPass = readSecret(props.adminPassword(), props.adminPasswordFile());

    try (var kc = KeycloakBuilder.builder()
            .serverUrl(props.baseUrl())
            .realm(props.adminRealm())
            .clientId("admin-cli")
            .grantType(OAuth2Constants.PASSWORD)
            .username(props.adminUsername())
            .password(adminPass)
            .build()) {

      var usersResource = kc.realm(props.targetRealm()).users();

      var existing = usersResource.searchByUsername(username, true);
      if (!existing.isEmpty()) {
        var id = UUID.fromString(existing.get(0).getId());
        log.info("KC user already exists username={} id={}", username, id);
        return new KeycloakProvisionResult(id, false);
      }

      var userRep = new UserRepresentation();
      userRep.setUsername(username);
      userRep.setEmail(email);
      userRep.setFirstName(firstName != null ? firstName : "");
      userRep.setLastName(lastName != null ? lastName : "");
      userRep.setEnabled(true);
      userRep.setEmailVerified(true);
      userRep.setAttributes(Map.of(
          "tenant_code", List.of(tenantCode),
          "plan", List.of(plan != null ? plan : "pro"),
          "featureSetId", List.of("base"),
          "locale", List.of("fr")));

      var response = usersResource.create(userRep);
      if (response.getStatus() != 201) {
        throw new IllegalStateException(
            "Failed to create KC user " + username + ": HTTP " + response.getStatus());
      }

      var location = response.getLocation().toString();
      var kcUserId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

      var cred = new CredentialRepresentation();
      cred.setType(CredentialRepresentation.PASSWORD);
      cred.setValue(tempPassword);
      cred.setTemporary(false);
      usersResource.get(kcUserId.toString()).resetPassword(cred);

      var roleRep = kc.realm(props.targetRealm()).roles().get(realmRole).toRepresentation();
      usersResource.get(kcUserId.toString()).roles().realmLevel().add(List.of(roleRep));

      log.info("KC user created username={} id={} role={}", username, kcUserId, realmRole);
      return new KeycloakProvisionResult(kcUserId, true);
    }
  }

  private static String readSecret(String value, String file) {
    try {
      if (file != null && !file.isBlank()) {
        return java.nio.file.Files.readString(java.nio.file.Path.of(file.trim())).trim();
      }
      if (value == null || value.isBlank()) {
        throw new IllegalStateException("Missing KC admin password");
      }
      return value.trim();
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot read KC admin password file", e);
    }
  }

  public record KeycloakProvisionResult(UUID keycloakId, boolean created) {}
}
