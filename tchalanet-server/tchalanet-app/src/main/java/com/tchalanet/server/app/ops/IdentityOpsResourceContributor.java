package com.tchalanet.server.app.ops;

import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.OpsResourceContributor;
import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.PlatformAdminOpsDashboardPayloadAssembler;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(120)
public class IdentityOpsResourceContributor implements OpsResourceContributor {

  private final Environment environment;

  public IdentityOpsResourceContributor(Environment environment) {
    this.environment = environment;
  }

  @Override
  public List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services() {
    String provider = property("tch.identity.provider", "firebase");
    String mode = identityMode(provider);
    String projectId = property("tch.identity.firebase.project-id", "");
    String revocationMode = property("tch.identity.firebase.revocation-check-mode", "sensitive-only");
    String emulatorHost = firstNonBlank(
        property("FIREBASE_AUTH_EMULATOR_HOST", ""),
        property("firebase.auth.emulator.host", ""));
    boolean production = isProductionProfile();

    IdentityStatus status = evaluate(provider, projectId, revocationMode, emulatorHost, production);
    String message = status.message()
        + " Provider=" + provider
        + ", mode=" + mode
        + ", revocation=" + revocationMode + ".";

    return List.of(new PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem(
        "identity:provider",
        "Identity provider",
        status.status(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        status.severity(),
        message,
        "/app/platform/ops/identity-sync",
        null,
        null,
        null));
  }

  private IdentityStatus evaluate(
      String provider,
      String projectId,
      String revocationMode,
      String emulatorHost,
      boolean production) {
    String normalizedProvider = normalize(provider);
    if ("firebase-emulator".equals(normalizedProvider)) {
      if (production) {
        return new IdentityStatus("INVALID", "CRITICAL", "Firebase emulator is active in a production profile.");
      }
      if (isBlank(emulatorHost)) {
        return new IdentityStatus("MISCONFIGURED", "CRITICAL", "Firebase emulator mode is active but emulator host is missing.");
      }
      return new IdentityStatus("EMULATOR", "OK", "Firebase emulator identity mode is configured for this environment.");
    }

    if ("firebase".equals(normalizedProvider)) {
      if (isBlank(projectId)) {
        return new IdentityStatus("MISCONFIGURED", "CRITICAL", "Firebase live mode is missing project id.");
      }
      if ("off".equals(normalize(revocationMode))) {
        return new IdentityStatus("LIVE", "WARNING", "Firebase live identity is configured, but revocation checks are off.");
      }
      return new IdentityStatus("LIVE", "OK", "Firebase live identity is configured.");
    }

    if ("local-jwt".equals(normalizedProvider)) {
      return production
          ? new IdentityStatus("INVALID", "CRITICAL", "Local JWT identity provider is active in a production profile.")
          : new IdentityStatus("LOCAL", "WARNING", "Local JWT identity provider is active.");
    }

    return new IdentityStatus("UNKNOWN", "WARNING", "Unknown identity provider configuration.");
  }

  private boolean isProductionProfile() {
    return Arrays.stream(environment.getActiveProfiles())
        .map(IdentityOpsResourceContributor::normalize)
        .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
  }

  private String property(String key, String defaultValue) {
    String value = environment.getProperty(key);
    return value == null ? defaultValue : value.trim();
  }

  private static String identityMode(String provider) {
    return switch (normalize(provider)) {
      case "firebase-emulator" -> "emulator";
      case "firebase" -> "live";
      case "local-jwt" -> "local";
      default -> "unknown";
    };
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private static String firstNonBlank(String first, String second) {
    return isBlank(first) ? second : first;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private record IdentityStatus(String status, String severity, String message) {}
}
