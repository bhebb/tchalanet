package com.tchalanet.server.common.security.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "kc.bootstrap",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class KeycloakBootstrapSyncListener {

  private final KeycloakBootstrapProperties props;
  private final JdbcTemplate jdbcTemplate;

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    if (!props.enabled()) {
      log.info("KC bootstrap sync disabled");
      return;
    }

    var adminPass = readSecret(props.adminPassword(), props.adminPasswordFile());

    try (var kc =
        KeycloakBuilder.builder()
            .serverUrl(props.baseUrl())
            .realm(props.adminRealm())
            .clientId("admin-cli")
            .grantType(OAuth2Constants.PASSWORD)
            .username(props.adminUsername())
            .password(adminPass)
            .build()) {

      for (var username : props.users()) {
        var kcIdStr =
            kc.realm(props.targetRealm()).users().searchByUsername(username, true).stream()
                .findFirst()
                .map(AbstractUserRepresentation::getId)
                .orElse(null);

        if (StringUtils.isBlank(kcIdStr)) {
          log.warn(
              "KC bootstrap sync: user not found in Keycloak realm={} username={}",
              props.targetRealm(),
              username);
          continue;
        }

        var kcId = UUID.fromString(kcIdStr);

        // Safety: avoid assigning same kcId to another user
        var conflict =
            jdbcTemplate.query(
                "select 1 from app_user where keycloak_sub = ? and deleted_at is null and username <> ? limit 1",
                ps -> {
                  ps.setObject(1, kcId);
                  ps.setString(2, username);
                },
                rs -> rs.next() ? 1 : null);
        if (conflict != null) {
          throw new IllegalStateException("Keycloak sub already assigned to another user: " + kcId);
        }

        int updated =
            jdbcTemplate.update(
                "update app_user set keycloak_sub = ?, updated_at = now() "
                    + "where username = ? and deleted_at is null and keycloak_sub <> ?",
                kcId,
                username,
                kcId);

        log.info("KC bootstrap sync: username={} kcId={} updatedRows={}", username, kcId, updated);
      }

      log.info("KC bootstrap sync done");
    }
  }

  private static String readSecret(String value, String file) {
    if (StringUtils.isBlank(file)) {
      try {
        return Files.readString(Path.of(file.trim())).trim();
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read secret file: " + file, e);
      }
    }
    if (StringUtils.isBlank(value)) {
      throw new IllegalStateException("Missing KC admin password");
    }
    return value.trim();
  }
}
