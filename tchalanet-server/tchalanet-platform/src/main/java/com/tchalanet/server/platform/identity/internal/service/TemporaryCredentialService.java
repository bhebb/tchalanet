package com.tchalanet.server.platform.identity.internal.service;

import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemporaryCredentialService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

  private final TemporaryCredentialProperties properties;
  private final Environment environment;

  public boolean adminTemporaryCredentialsEnabled() {
    return properties.admin().effectiveEnabled();
  }

  public String adminTemporaryPassword() {
    if (isProduction() && properties.admin().effectiveGenerateInProd()) {
      return "Tcha-" + (1000 + RANDOM.nextInt(9000)) + "-" + randomLetters(2) + "!";
    }
    return properties.admin().effectiveDefaultPassword();
  }

  private boolean isProduction() {
    for (var profile : environment.getActiveProfiles()) {
      if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
        return true;
      }
    }
    return false;
  }

  private static String randomLetters(int length) {
    var builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
    }
    return builder.toString();
  }
}
