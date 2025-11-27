package com.tchalanet.server.external.infra;

import com.tchalanet.server.external.ports.KeycloakUserProvisioningPort;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class KeycloakUserProvisioningHttpAdapter implements KeycloakUserProvisioningPort {

  private final String adminUrl = "http://localhost:8080"; // override via properties

  @Override
  public Map<String, Object> createUser(Map<String, Object> payload) {
    String url = adminUrl + "/admin/realms/tchalanet/users";
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, h);
    ResponseEntity<Void> r = null; // rest.postForEntity(url, req, Void.class);
    return Map.of("status", r.getStatusCode());
  }

  @Override
  public void resetPassword(String userId, String newPassword) {
    // TODO: implement using Keycloak admin API
  }
}
