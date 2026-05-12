package com.tchalanet.server.platform.identity.internal.service;

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
@ConditionalOnProperty(prefix = "kc.bootstrap", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KeycloakBootstrapSyncListener {

    private final KeycloakBootstrapProperties props;
    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {

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

                var kcId =
                    kc.realm(props.targetRealm())
                        .users()
                        .searchByUsername(username, true)
                        .stream()
                        .findFirst()
                        .map(AbstractUserRepresentation::getId)
                        .map(UUID::fromString)
                        .orElse(null);

                if (kcId == null) {
                    log.warn("KC user not found: {}", username);
                    continue;
                }

                int updated =
                    jdbc.update(
                        """
                        update app_user
                        set keycloak_sub = ?, updated_at = now()
                        where username = ?
                          and deleted_at is null
                        """,
                        kcId,
                        username);

                log.info("KC sync: username={} kcId={} updated={}", username, kcId, updated);
            }

            log.info("KC bootstrap sync done");
        }
    }

    private static String readSecret(String value, String file) {
        try {
            if (StringUtils.isNotBlank(file)) {
                return Files.readString(Path.of(file.trim())).trim();
            }
            if (StringUtils.isBlank(value)) {
                throw new IllegalStateException("Missing KC admin password");
            }
            return value.trim();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read secret file", e);
        }
    }
}
