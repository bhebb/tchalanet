package com.tchalanet.server.platform.identity.internal.local;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
final class LocalIdentityProductionGuard {

  private static final Set<String> FORBIDDEN_PRODUCTION_PROVIDERS = Set.of("local-jwt", "local-perf", "keycloak");

  private final String provider;
  private final Environment environment;

  LocalIdentityProductionGuard(
      @Value("${tch.identity.provider:firebase}") String provider, Environment environment) {
    this.provider = provider;
    this.environment = environment;
  }

  @PostConstruct
  void validate() {
    if (isProductionProfile() && FORBIDDEN_PRODUCTION_PROVIDERS.contains(provider)) {
      throw new IllegalStateException(
          "Identity provider '" + provider + "' is forbidden in production");
    }
  }

  private boolean isProductionProfile() {
    return Arrays.stream(environment.getActiveProfiles())
        .map(String::toLowerCase)
        .anyMatch(profile -> profile.contains("prod"));
  }
}
