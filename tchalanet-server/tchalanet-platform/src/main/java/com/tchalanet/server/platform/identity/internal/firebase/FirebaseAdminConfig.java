package com.tchalanet.server.platform.identity.internal.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnExpression(
    "'${tch.identity.provider:firebase}' == 'firebase' || '${tch.identity.provider:firebase}' == 'firebase-emulator'")
class FirebaseAdminConfig {

  private static final String APP_NAME = "tchalanet-identity";

  @Bean
  @Lazy
  FirebaseAuth firebaseAuth(
      FirebaseIdentityProperties properties,
      @Value("${tch.identity.provider:firebase}") String provider,
      @Value("${FIREBASE_AUTH_EMULATOR_HOST:}") String emulatorHost) {
    validateEmulatorHost(provider, emulatorHost);
    var existingApp =
        FirebaseApp.getApps().stream()
            .filter(app -> APP_NAME.equals(app.getName()))
            .findFirst();
    if (existingApp.isPresent()) {
      return FirebaseAuth.getInstance(existingApp.orElseThrow());
    }

    var options =
        FirebaseOptions.builder()
            .setProjectId(properties.requiredProjectId())
            .setCredentials(credentials(provider, properties.credentialsPath()))
            .build();
    return FirebaseAuth.getInstance(FirebaseApp.initializeApp(options, APP_NAME));
  }

  static void validateEmulatorHost(String provider, String emulatorHost) {
    if ("firebase-emulator".equals(provider)
        && (emulatorHost == null || emulatorHost.isBlank())) {
      throw new IllegalStateException(
          "firebase-emulator requires FIREBASE_AUTH_EMULATOR_HOST, for example localhost:9099");
    }
  }

  private static GoogleCredentials credentials(String provider, String credentialsPath) {
    try {
      if ("firebase-emulator".equals(provider)
          && (credentialsPath == null || credentialsPath.isBlank())) {
        return GoogleCredentials.create(
            new AccessToken("firebase-emulator-owner", Date.from(Instant.now().plusSeconds(3600))));
      }
      if (credentialsPath == null || credentialsPath.isBlank()) {
        return GoogleCredentials.getApplicationDefault();
      }
      try (InputStream input = Files.newInputStream(Path.of(credentialsPath.trim()))) {
        return GoogleCredentials.fromStream(input);
      }
    } catch (IOException ex) {
      throw new IllegalStateException("Firebase Admin credentials could not be loaded", ex);
    }
  }
}
