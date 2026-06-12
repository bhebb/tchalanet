package com.tchalanet.server.platform.identity.internal.service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
final class AppUserBootstrapProductionGuard {

  private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");

  private final UserBootstrapProperties properties;
  private final Environment environment;

  AppUserBootstrapProductionGuard(UserBootstrapProperties properties, Environment environment) {
    this.properties = properties;
    this.environment = environment;
  }

  @PostConstruct
  void validate() {
    if (properties.effectiveMode() != AppUserBootstrapMode.CONTROLLED_AUTO || !isProduction()) {
      return;
    }
    if (!properties.allowControlledAutoInProduction() || !hasControlledAutoAllowlist()) {
      throw new IllegalStateException(
          "controlled-auto AppUser bootstrap is forbidden in production without explicit approval and an allowlist");
    }
  }

  private boolean isProduction() {
    return Arrays.stream(environment.getActiveProfiles())
        .map(String::toLowerCase)
        .anyMatch(PRODUCTION_PROFILES::contains);
  }

  private boolean hasControlledAutoAllowlist() {
    return (properties.controlledAutoAllowedEmails() != null
            && !properties.controlledAutoAllowedEmails().isEmpty())
        || (properties.controlledAutoAllowedDomains() != null
            && !properties.controlledAutoAllowedDomains().isEmpty());
  }
}
