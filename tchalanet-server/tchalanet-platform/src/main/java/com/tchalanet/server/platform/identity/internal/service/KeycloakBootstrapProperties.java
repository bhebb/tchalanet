package com.tchalanet.server.platform.identity.internal.service;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kc.bootstrap")
@Validated
public record KeycloakBootstrapProperties(

    boolean enabled,

    @NotBlank String baseUrl,
    @NotBlank String targetRealm,

    String adminRealm,

    @NotBlank String adminUsername,
    String adminPassword,
    String adminPasswordFile,

    // Password assigned to users provisioned in Keycloak during onboarding. Non-temporary so a
    // freshly onboarded tenant admin / cashier can log in immediately (dev/E2E). Override per env.
    String defaultUserPassword,

    List<String> users

) {
    public KeycloakBootstrapProperties {
        if (adminRealm == null || adminRealm.isBlank()) {
            adminRealm = "master";
        }
        if (defaultUserPassword == null || defaultUserPassword.isBlank()) {
            defaultUserPassword = "Changeme1!";
        }
        if (users == null || users.isEmpty()) {
            users = List.of("super_admin", "admin");
        }
    }
}
