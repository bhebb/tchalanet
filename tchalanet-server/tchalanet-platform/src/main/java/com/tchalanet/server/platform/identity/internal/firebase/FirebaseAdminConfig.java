package com.tchalanet.server.platform.identity.internal.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnProperty(prefix = "tch.identity", name = "provider", havingValue = "firebase")
class FirebaseAdminConfig {

  private static final String APP_NAME = "tchalanet-identity";

  @Bean
  @Lazy
  FirebaseAuth firebaseAuth(FirebaseIdentityProperties properties) {
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
            .setCredentials(credentials(properties.credentialsPath()))
            .build();
    return FirebaseAuth.getInstance(FirebaseApp.initializeApp(options, APP_NAME));
  }

  private static GoogleCredentials credentials(String credentialsPath) {
    try {
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
