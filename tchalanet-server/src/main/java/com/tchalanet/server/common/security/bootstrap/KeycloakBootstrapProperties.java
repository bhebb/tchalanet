package com.tchalanet.server.common.security.bootstrap;

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

    List<String> users

) {
    public KeycloakBootstrapProperties {
        if (adminRealm == null || adminRealm.isBlank()) {
            adminRealm = "master";
        }
        if (users == null || users.isEmpty()) {
            users = List.of("super_admin", "admin");
        }
    }
}
