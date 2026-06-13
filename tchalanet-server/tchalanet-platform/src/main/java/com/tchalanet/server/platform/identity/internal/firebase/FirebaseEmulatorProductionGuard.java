package com.tchalanet.server.platform.identity.internal.firebase;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
final class FirebaseEmulatorProductionGuard {

  private final String provider;
  private final Environment environment;

  FirebaseEmulatorProductionGuard(
      @Value("${tch.identity.provider:firebase}") String provider, Environment environment) {
    this.provider = provider;
    this.environment = environment;
  }

  @PostConstruct
  void validate() {
    if ("firebase-emulator".equals(provider)
        && Arrays.stream(environment.getActiveProfiles())
            .map(String::toLowerCase)
            .anyMatch(profile -> profile.contains("prod"))) {
      throw new IllegalStateException("Firebase Auth Emulator is forbidden in production");
    }
  }
}
