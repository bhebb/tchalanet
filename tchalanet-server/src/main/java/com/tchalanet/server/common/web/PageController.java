package com.tchalanet.server.common.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.domain.TchRole;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API pour servir les configurations de pages statiques. Migration hexagonale - ce contrôleur
 * reste dans common car il sert une fonction technique.
 */
@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
@Slf4j
public class PageController {

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;

  @GetMapping(value = "/home-public", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPublicHomePage() {
    return serveStaticJson("classpath:static/config/home-public.json");
  }

  @GetMapping(value = "/private", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPrivatePage(@CurrentContext TchRequestContext ctx) {
    var roles = ctx.systemRoles();

    // Si SUPER_ADMIN avec override tenant → page tenant admin
    if (roles.contains(TchRole.SUPER_ADMIN)
        && ctx.tenantOverridden()
        && !ctx.tenantId().equals(ctx.originalTenantId())) {
      return serveStaticJson("classpath:static/config/home-super-admin.json");
    }

    // Sinon, retourne la page selon le rôle
    if (roles.contains(TchRole.SUPER_ADMIN))
      return serveStaticJson("classpath:static/config/home-super-admin.json");
    if (roles.contains(TchRole.ADMIN))
      return serveStaticJson("classpath:static/config/home-tenant-admin.json");
    if (roles.contains(TchRole.CASHIER))
      return serveStaticJson("classpath:static/config/home-cashier.json");

    return serveStaticJson("classpath:static/config/home-tenant-admin.json");
  }

  @GetMapping(value = "/private/{profile}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPrivatePageForProfile(@PathVariable String profile) {
    String normalized = profile.trim().toLowerCase(Locale.ROOT);
    String file =
        switch (normalized) {
          case "sa", "super", "super-admin" -> "config/home-super-admin.json";
          case "ta", "tenant-admin", "admin-tenant" -> "config/home-tenant-admin.json";
          case "cashier", "vendeur" -> "config/home-cashier.json";
          default -> "config/private-fallback.json";
        };
    return serveStaticJson(file);
  }

  private ResponseEntity<String> serveStaticJson(String classpathLocation) {
    String body = readFromClasspathOrNull(classpathLocation);
    if (body == null) {
      // fallback ultime
      body = readFromClasspathOrNull("config/private-fallback.json");
      if (body == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(APPLICATION_JSON)
            .body("{\"error\":\"Configuration not found\"}");
      }
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic());
    return ResponseEntity.ok().headers(headers).contentType(APPLICATION_JSON).body(body);
  }

  private String readFromClasspathOrNull(String location) {
    try {
      Resource resource = resourceLoader.getResource(location);
      if (!resource.exists()) {
        log.warn("Resource not found: {}", location);
        return null;
      }
      byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
      String json = new String(bytes, StandardCharsets.UTF_8);

      // Validation JSON optionnelle
      try {
        objectMapper.readTree(json);
      } catch (IOException e) {
        log.error("Invalid JSON in resource {}: {}", location, e.getMessage());
        return null;
      }

      return json;
    } catch (IOException e) {
      log.error("Error reading resource {}: {}", location, e.getMessage());
      return null;
    }
  }
}
