package com.tchalanet.server.external.infra;

import com.tchalanet.server.external.ports.KeycloakUserProvisioningPort;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeycloakUserProvisioningHttpAdapter implements KeycloakUserProvisioningPort {

  private final RestTemplate rest;
  private final String adminUrl = "http://localhost:8080"; // override via properties

  public KeycloakUserProvisioningHttpAdapter(RestTemplateBuilder b) {
    this.rest = b.build();
  }

  @Override
  public Map<String, Object> createUser(Map<String, Object> payload) {
    String url = adminUrl + "/admin/realms/tchalanet/users";
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, h);
    ResponseEntity<Void> r = rest.postForEntity(url, req, Void.class);
    return Map.of("status", r.getStatusCodeValue());
  }

  @Override
  public void resetPassword(String userId, String newPassword) {
    // TODO: implement using Keycloak admin API
  }
}
