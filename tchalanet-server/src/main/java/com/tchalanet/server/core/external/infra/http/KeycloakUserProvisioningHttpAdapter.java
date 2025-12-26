package com.tchalanet.server.core.external.infra.http;

import com.tchalanet.server.core.external.port.out.KeycloakUserProvisioningPort;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class KeycloakUserProvisioningHttpAdapter implements KeycloakUserProvisioningPort {

  private static final String USERS_PATH = "/admin/realms/tchalanet/users";

  private final WebClient webClient;
  private final String adminUrl;

  public KeycloakUserProvisioningHttpAdapter(
      WebClient webClient,
      @Value("${keycloak.admin.server-url:http://localhost:8080}") String adminUrl) {
    this.webClient = webClient;
    this.adminUrl = adminUrl;
  }

  @Override
  public Map<String, Object> createUser(Map<String, Object> payload) {
    String url = adminUrl + USERS_PATH;

    try {
      String response =
          webClient
              .post()
              .uri(url)
              .contentType(MediaType.APPLICATION_JSON)
              .body(BodyInserters.fromValue(payload))
              .retrieve()
              .bodyToMono(String.class) // Could be improved with a proper DTO
              .block();

      if (response == null) {
        log.warn("Keycloak user creation returned empty body for payload: {}", payload);
        return Map.of("status", "success", "response", "");
      }

      log.info("Successfully created user in Keycloak. Response: {}", response);
      return Map.of("status", "success", "response", response);

    } catch (Exception e) {
      log.error("Keycloak user creation failed", e);
      return Map.of("error", e.getMessage());
    }
  }

  @Override
  public void resetPassword(String userId, String newPassword) {
    String url = adminUrl + USERS_PATH + "/" + userId + "/reset-password";

    Map<String, Object> body = Map.of("type", "password", "temporary", false, "value", newPassword);

    try {
      webClient
          .put()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(body))
          .retrieve()
          .toBodilessEntity()
          .block();

      log.info("Successfully reset password for user {}", userId);
    } catch (Exception e) {
      log.error("Keycloak password reset failed for user {}", userId, e);
    }
  }

  @Override
  public void updateUserProfile(
      UUID keycloakId, String firstName, String lastName, String email, String locale) {
    String url = adminUrl + USERS_PATH + "/" + keycloakId;

    Map<String, Object> body = new HashMap<>();
    body.put("firstName", firstName);
    body.put("lastName", lastName);
    body.put("email", email);
    if (locale != null && !locale.isBlank()) {
      body.put("attributes", Map.of("locale", locale));
    }

    try {
      webClient
          .put()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(body))
          .retrieve()
          .toBodilessEntity()
          .block();

      log.info("Successfully updated profile for user {}", keycloakId);
    } catch (Exception e) {
      log.error("Keycloak profile update failed for user {}", keycloakId, e);
    }
  }

  @Override
  public void disableUser(UUID keycloakId, String reason) {
    String url = adminUrl + USERS_PATH + "/" + keycloakId;

    Map<String, Object> body = new HashMap<>();
    body.put("enabled", false);
    if (reason != null && !reason.isBlank()) {
      body.put("attributes", Map.of("disableReason", reason));
    }

    try {
      webClient
          .put()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(body))
          .retrieve()
          .toBodilessEntity()
          .block();

      log.info("Successfully disabled user {} for reason: {}", keycloakId, reason);
    } catch (Exception e) {
      log.error("Keycloak disable user failed for user {}", keycloakId, e);
    }
  }
}
