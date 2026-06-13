package com.tchalanet.server.platform.identity.internal.local;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
final class LocalIdentityProductionGuard {

  private static final Set<String> LOCAL_PROVIDERS = Set.of("local-jwt", "local-perf");

  private final String provider;
  private final Environment environment;

  LocalIdentityProductionGuard(
      @Value("${tch.identity.provider:firebase}") String provider, Environment environment) {
    this.provider = provider;
    this.environment = environment;
  }

  @PostConstruct
  void validate() {
    if (LOCAL_PROVIDERS.contains(provider)
        && Arrays.stream(environment.getActiveProfiles())
            .map(String::toLowerCase)
            .anyMatch(profile -> profile.contains("prod"))) {
      throw new IllegalStateException(
          "Local identity provider '" + provider + "' is forbidden in production");
    }
  }
}
