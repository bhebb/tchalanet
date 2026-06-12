package com.tchalanet.server.platform.identity.internal.service;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.security.user-bootstrap")
public record UserBootstrapProperties(
    boolean enabled,
    boolean updateLastLogin,
    AppUserBootstrapMode mode,
    List<String> controlledAutoAllowedEmails,
    List<String> controlledAutoAllowedDomains,
    boolean allowControlledAutoInProduction) {

  public AppUserBootstrapMode effectiveMode() {
    return mode == null ? AppUserBootstrapMode.DENY : mode;
  }

  public boolean controlledAutoAllows(String email) {
    if (email == null || email.isBlank()) {
      return false;
    }
    var normalized = email.trim().toLowerCase(Locale.ROOT);
    var emails = controlledAutoAllowedEmails == null ? List.<String>of() : controlledAutoAllowedEmails;
    var domains =
        controlledAutoAllowedDomains == null ? List.<String>of() : controlledAutoAllowedDomains;
    return emails.stream()
        .map(String::trim)
        .map(value -> value.toLowerCase(Locale.ROOT))
        .anyMatch(normalized::equals)
        || domains.stream()
            .map(String::trim)
            .map(value -> value.toLowerCase(Locale.ROOT))
            .map(domain -> domain.startsWith("@") ? domain : "@" + domain)
            .anyMatch(normalized::endsWith);
  }
}
