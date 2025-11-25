package com.tchalanet.server.common.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.domain.TchRole;
import com.tchalanet.server.common.usecase.GetPublicHomePageUseCase;
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

/** REST API pour servir les configurations de pages (public/privé). */
@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
@Slf4j
public class PageController {

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;
  private final GetPublicHomePageUseCase getPublicHomePageUseCase;

  /**
   * Page d'accueil publique. Pour l'instant, on sert le JSON statique {@code home-public.json}. Par
   * la suite, on pourra enrichir le JSON avec des données calculées (plans, jeux, tirages du jour,
   * prochain tirage) en fonction du tenant courant (résolu via {@link TchRequestContext}).
   */
  @GetMapping(value = "/home-public", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getPublicHomePage(@CurrentContext TchRequestContext ctx) {
    // On garde le fichier statique comme base
    var baseJson = readFromClasspathOrNull("classpath:static/config/home-public.json");
    if (baseJson == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .contentType(APPLICATION_JSON)
          .body("{\"error\":\"Configuration not found\"}");
    }

    // Orchestration des données dynamiques via use case
    var tenantUuid = ctx != null ? ctx.tenantUuid() : null;
    var data = getPublicHomePageUseCase.get(tenantUuid);

    try {
      JsonNode root = objectMapper.readTree(baseJson);
      if (root.isObject()) {
        var obj = (ObjectNode) root;
        var dataNode = obj.with("data");
        // On injecte la structure telle quelle, sans figer le contrat JSON ici.
        dataNode.put("tenantId", data.tenantId() != null ? data.tenantId().toString() : null);
        dataNode.set("plans", objectMapper.valueToTree(data.plans()));
        dataNode.set("games", objectMapper.valueToTree(data.games()));
        dataNode.set("drawsToday", objectMapper.valueToTree(data.drawsToday()));
        dataNode.set("nextDraw", objectMapper.valueToTree(data.nextDraw()));
        baseJson = objectMapper.writeValueAsString(root);
      }
    } catch (IOException e) {
      log.error("Failed to enrich public home JSON", e);
      // en cas d'erreur, on renvoie simplement la version statique
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic());
    return ResponseEntity.ok().headers(headers).contentType(APPLICATION_JSON).body(baseJson);
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
    var normalized = profile.trim().toLowerCase(Locale.ROOT);
    var file =
        switch (normalized) {
          case "sa", "super", "super-admin" -> "config/home-super-admin.json";
          case "ta", "tenant-admin", "admin-tenant" -> "config/home-tenant-admin.json";
          case "cashier", "vendeur" -> "config/home-cashier.json";
          default -> "config/private-fallback.json";
        };
    return serveStaticJson(file);
  }

  private ResponseEntity<String> serveStaticJson(String classpathLocation) {
    var body = readFromClasspathOrNull(classpathLocation);
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
      var json = new String(bytes, StandardCharsets.UTF_8);

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
