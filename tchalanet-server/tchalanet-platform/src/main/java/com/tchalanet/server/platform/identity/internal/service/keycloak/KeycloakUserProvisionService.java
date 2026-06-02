package com.tchalanet.server.platform.identity.internal.service.keycloak;

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
      String realmRole) {

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

      var attributes = new java.util.HashMap<String, List<String>>();
      // Roles/permissions are resolved app-side from tenant_user, not Keycloak realm roles.
      // The one claim the runtime needs from the token is tenant_code (mapped from this
      // attribute) so RLS/tenant resolution works on the new user's first login.
      if (tenantCode != null && !tenantCode.isBlank()) {
        attributes.put("tenant_code", List.of(tenantCode));
      }
      attributes.put("plan", List.of(plan != null ? plan : "pro"));
      attributes.put("featureSetId", List.of("base"));
      attributes.put("locale", List.of("fr"));

      var userRep = new UserRepresentation();
      userRep.setUsername(username);
      userRep.setEmail(email);
      // The realm enables VERIFY_PROFILE, which Keycloak evaluates dynamically at login: a user
      // missing a required profile attribute (firstName/lastName) is rejected with "Account is
      // not fully set up" even when no required action is stored. Onboarding may not supply names,
      // so fall back to non-blank values derived from the email/username.
      userRep.setFirstName(firstNameOrDerived(firstName, username, email));
      userRep.setLastName(lastNameOrDefault(lastName));
      userRep.setEnabled(true);
      userRep.setEmailVerified(true);
      // Clear any realm-default required actions (VERIFY_EMAIL, UPDATE_PASSWORD, ...) so the
      // provisioned user can authenticate immediately.
      userRep.setRequiredActions(List.of());
      userRep.setAttributes(attributes);

      var response = usersResource.create(userRep);
      if (response.getStatus() != 201) {
        throw new IllegalStateException(
            "Failed to create KC user " + username + ": HTTP " + response.getStatus());
      }

      var location = response.getLocation().toString();
      var kcUserId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

      var cred = new CredentialRepresentation();
      cred.setType(CredentialRepresentation.PASSWORD);
      cred.setValue(props.defaultUserPassword());
      cred.setTemporary(false);
      usersResource.get(kcUserId.toString()).resetPassword(cred);

      // Realm-role assignment is optional: this platform derives authorization from the app's
      // tenant_user.role_id, not Keycloak realm roles (the realm only defines
      // platform.tenant.override). Only assign when an existing realm role is explicitly named.
      if (realmRole != null && !realmRole.isBlank()) {
        var roleRep = kc.realm(props.targetRealm()).roles().get(realmRole).toRepresentation();
        usersResource.get(kcUserId.toString()).roles().realmLevel().add(List.of(roleRep));
      }

      log.info("KC user created username={} id={} role={}", username, kcUserId, realmRole);
      return new KeycloakProvisionResult(kcUserId, true);
    }
  }

  /**
   * Assigns an existing realm role to a Keycloak user (best-effort, idempotent).
   *
   * <p>API-created users get their app role in {@code tenant_user_role}, but the JWT
   * authorities are derived from Keycloak realm roles (see SecurityConfig). Without this
   * a created cashier/admin carries no authority and is rejected (403) on every
   * role-gated endpoint. No-op when bootstrap is disabled or the role/user is unknown;
   * never throws, so it cannot break user creation.
   */
  public void assignRealmRole(UUID kcUserId, String roleName) {
    if (!props.enabled() || kcUserId == null || roleName == null || roleName.isBlank()) {
      return;
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
      var roleRep = kc.realm(props.targetRealm()).roles().get(roleName).toRepresentation();
      kc.realm(props.targetRealm()).users().get(kcUserId.toString())
          .roles().realmLevel().add(List.of(roleRep));
      log.info("KC realm role '{}' assigned to user {}", roleName, kcUserId);
    } catch (Exception e) {
      log.warn("Could not assign KC realm role '{}' to user {}: {}", roleName, kcUserId, e.getMessage());
    }
  }

  private static String firstNameOrDerived(String firstName, String username, String email) {
    if (firstName != null && !firstName.isBlank()) {
      return firstName.trim();
    }
    var source = (email != null && !email.isBlank()) ? email : username;
    if (source == null || source.isBlank()) {
      return "User";
    }
    // local part before '@' and before any '+' tag, e.g. "admin+kc12@x.test" -> "admin".
    var local = source.split("@", 2)[0].split("\\+", 2)[0].trim();
    return local.isBlank() ? "User" : local;
  }

  private static String lastNameOrDefault(String lastName) {
    return (lastName != null && !lastName.isBlank()) ? lastName.trim() : "User";
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
