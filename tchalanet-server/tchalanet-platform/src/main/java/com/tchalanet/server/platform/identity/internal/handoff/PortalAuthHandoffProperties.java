package com.tchalanet.server.platform.identity.internal.handoff;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.portal-auth-handoff")
public record PortalAuthHandoffProperties(
    Duration ttl, Map<PortalTarget, String> portalBaseUrls, List<String> allowedEntryRoutes) {
  private static final Duration DEFAULT_TTL = Duration.ofSeconds(60);
  private static final Map<PortalTarget, String> DEFAULT_PORTAL_BASE_URLS =
      Map.of(
          PortalTarget.ADMIN, "/admin",
          PortalTarget.PLATFORM, "/platform");
  private static final List<String> DEFAULT_ALLOWED_ENTRY_ROUTES =
      List.of(
          "/app/admin",
          "/app/platform",
          "/app/account/activation",
          "/app/profile",
          "/app/seller-terminal/activation");

  public PortalAuthHandoffProperties {
    ttl = ttl == null || ttl.isNegative() || ttl.isZero() ? DEFAULT_TTL : ttl;
    portalBaseUrls =
        portalBaseUrls == null || portalBaseUrls.isEmpty()
            ? DEFAULT_PORTAL_BASE_URLS
            : Map.copyOf(portalBaseUrls);
    allowedEntryRoutes =
        allowedEntryRoutes == null || allowedEntryRoutes.isEmpty()
            ? DEFAULT_ALLOWED_ENTRY_ROUTES
            : List.copyOf(allowedEntryRoutes);
  }

  String targetUrl(PortalTarget target) {
    var value = portalBaseUrls.get(target);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing portal base URL for " + target);
    }
    return withoutTrailingSlash(value.trim());
  }

  boolean isAllowedEntryRoute(String route) {
    if (route == null || route.isBlank() || !route.startsWith("/") || route.startsWith("//")) {
      return false;
    }
    return allowedEntryRoutes.stream()
        .anyMatch(allowed -> route.equals(allowed) || route.startsWith(allowed + "/"));
  }

  private static String withoutTrailingSlash(String value) {
    return value.length() > 1 ? value.replaceAll("/+$", "") : value;
  }
}
