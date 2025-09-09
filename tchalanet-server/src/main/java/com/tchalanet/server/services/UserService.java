package com.tchalanet.server.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  @Autowired private JdbcTemplate jdbc;

  public void upsertUserFromJwt(Jwt jwt) {
    UUID id = UUID.fromString(jwt.getSubject());
    String username = jwt.getClaimAsString("preferred_username");
    String email = jwt.getClaimAsString("email");
    Object tenantInToken =
        jwt.getClaim("tenants"); // si JSON, spring la mappe déjà en type Map/List

    ObjectMapper objectMapper = new ObjectMapper();
    String tenant = "tenant-dev";
    if (tenantInToken instanceof Map<?, ?> || tenantInToken instanceof Collection) {
      try {
        tenant = objectMapper.writeValueAsString(tenantInToken);
      } catch (JsonProcessingException e) {
      }
    }

    String activeEnt = jwt.getClaimAsString("active_enterprise_id");

    jdbc.update(
        """
                        INSERT INTO app_user (id, username, email, tenant, active_enterprise_id, last_login_at)
                        VALUES (?,?,?,?,?,now())
                        ON CONFLICT (id) DO UPDATE
                          SET username = EXCLUDED.username,
                              email = EXCLUDED.email,
                              tenant = EXCLUDED.tenant,
                              active_enterprise_id = EXCLUDED.active_enterprise_id,
                              last_login_at = now()
                        """,
        id,
        username,
        email,
        tenant,
        activeEnt);
  }
}
