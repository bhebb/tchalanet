package com.tchalanet.server.api;

import static com.tchalanet.server.constants.TchRole.ADMIN;
import static com.tchalanet.server.constants.TchRole.CASHIER;
import static com.tchalanet.server.constants.TchRole.SUPER_ADMIN;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.config.context.CurrentContext;
import com.tchalanet.server.config.context.RequestContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
@Slf4j
public class PageController {

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;

  // ---------- PUBLIC ----------

  @GetMapping(value = "/home-public", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPublicHomePage() {
    return serveStaticJson("classpath:static/config/home-public.json");
  }

  // ---------- PRIVATE (auto, en fonction des rôles) ----------
  @GetMapping(value = "/private", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPrivatePage(@CurrentContext RequestContext ctx) {
    var roles = ctx.systemRoles();

    // ✅ If SA + overridden → open tenant page (tenant-admin shell)
    if (roles.contains(SUPER_ADMIN)
        && ctx.tenantOverridden()
        && !ctx.tenantId().equals(ctx.originalTenantId())) {
      return serveStaticJson("classpath:static/config/private-super-admin.json");
    }

    // ✅ Otherwise: return matched page by role
    if (roles.contains(SUPER_ADMIN))
      return serveStaticJson("classpath:static/config/private-super-admin.json");
    if (roles.contains(ADMIN))
      return serveStaticJson("classpath:static/config/private-tenant-admin.json");
    if (roles.contains(CASHIER))
      return serveStaticJson("classpath:static/config/private-cashier.json");

    return serveStaticJson("classpath:static/config/private-fallback.json");
  }

  // ---------- PRIVATE (profil explicite pour tests / snapshots / QA) ----------

  @GetMapping(value = "/private/{profile}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPrivatePageForProfile(@PathVariable String profile) {
    String normalized = profile.trim().toLowerCase(Locale.ROOT);
    String file =
        switch (normalized) {
          case "sa", "super", "super-admin" -> "config/private-super-admin.json";
          case "ta", "tenant-admin", "admin-tenant" -> "config/private-tenant-admin.json";
          case "cashier", "vendeur" -> "config/private-cashier.json";
          default -> "config/private-fallback.json";
        };
    return serveStaticJson(file);
  }

  // ---------- Utils ----------

  private ResponseEntity<String> serveStaticJson(String classpathLocation) {
    String body = readFromClasspathOrNull(classpathLocation);
    if (body == null) {
      // fallback ultime
      body = readFromClasspathOrNull("config/private-fallback.json");
      if (body == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(APPLICATION_JSON)
            .body("{\"error\":\"page_config_not_found\"}");
      }
    }

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CACHE_CONTROL,
            CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic().getHeaderValue())
        .contentType(APPLICATION_JSON)
        .body(body);
  }

  private String extractTenantId(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      if (jwt == null) return null;
      // example: custom claim "tch" : { tenantId: "..." }
      Map<String, Object> tch = jwt.getClaim("tch");
      if (tch != null) {
        Object tid = tch.get("tenantId");
        return tid != null ? String.valueOf(tid) : null;
      }
      // or a flat claim "tenant_id"
      Object tid = jwt.getClaim("tenant_id");
      return tid != null ? String.valueOf(tid) : null;
    }
    return null;
  }

  private String readFromClasspathOrNull(String classpathLocation) {
    try {
      log.debug("Loading page fom classpath:static/config");
      Resource resource = resourceLoader.getResource(classpathLocation);
      if (!resource.exists()) return null;
      byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.info("No i18n overrides found");
    }
    return null;
  }

  /**
   * Résout un "profil" lisible (super-admin / tenant-admin / cashier) à partir des rôles du JWT. On
   * traite d'abord les rôles "forts" pour éviter les collisions.
   */
  private String resolveProfileFromAuth(Authentication authentication) {
    Set<String> roles = extractRoles(authentication);

    // Priorité forte -> faible
    if (roles.contains("SUPER_ADMIN")) return "super-admin";
    if (roles.contains("TENANT_ADMIN")) return "tenant-admin";
    // ex. 'CASHIER' / 'VENDEUR' : adapte à tes noms réels
    if (roles.contains("CASHIER") || roles.contains("VENDEUR") || roles.contains("SELLER"))
      return "cashier";

    // défaut
    return "cashier";
  }

  /**
   * Extrait les rôles depuis: - realm_access.roles (Keycloak) - resource_access.{client}.roles (si
   * tu utilises des rôles par client)
   */
  @SuppressWarnings("unchecked")
  private Set<String> extractRoles(Authentication authentication) {
    Set<String> out = new HashSet<>();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return out;
    }
    Jwt jwt = jwtAuth.getToken();
    if (jwt == null) return out;

    // realm_access.roles
    Object ra = jwt.getClaim("realm_access");
    if (ra instanceof Map<?, ?> ram) {
      Object rolesObj = ram.get("roles");
      if (rolesObj instanceof Collection<?> coll) {
        coll.forEach(
            r -> {
              if (r != null) out.add(String.valueOf(r));
            });
      }
    }

    // resource_access.{client}.roles
    Object resAcc = jwt.getClaim("resource_access");
    if (resAcc instanceof Map<?, ?> ram) {
      for (Object clientNameObj : ram.keySet()) {
        Object val = ram.get(clientNameObj);
        if (val instanceof Map<?, ?> m2) {
          Object r2 = m2.get("roles");
          if (r2 instanceof Collection<?> coll) {
            coll.forEach(
                r -> {
                  if (r != null) out.add(String.valueOf(r));
                });
          }
        }
      }
    }

    // Uniformise (MAJ -> MAJ), supprime doublons
    Set<String> upper = new HashSet<>();
    for (String r : out) upper.add(r.toUpperCase(Locale.ROOT));
    return upper;
  }
}
