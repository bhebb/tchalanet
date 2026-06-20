package com.tchalanet.server.platform.identity.internal.firebase;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.identity.firebase.bootstrap")
public record FirebaseBootstrapProperties(
    boolean enabled, boolean autoRunOnStartup, String defaultUserPassword, List<String> users) {

  public FirebaseBootstrapProperties {
    if (defaultUserPassword == null || defaultUserPassword.isBlank()) {
      defaultUserPassword = "Changeme1!";
    }
    if (users == null || users.isEmpty()) {
      users = List.of("super_admi@localtest.me", "admin@localtest.me");
    }
  }
}
