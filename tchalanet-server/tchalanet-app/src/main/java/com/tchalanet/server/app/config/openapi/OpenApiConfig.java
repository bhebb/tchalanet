package com.tchalanet.server.app.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Value("${app.base-path:/api}")
  private String basePath;

  @Value("${app.api-version:v1}")
  private String apiVersion;

  @Value("${app.api-base-url:http://localhost:8083}")
  private String apiBaseUrl;

  @Bean
  public OpenAPI customOpenAPI() {
    var prefix = normalizePrefix(basePath);
    var serverUrl = normalizeUrl(apiBaseUrl + prefix + "/" + apiVersion);

    var bearerScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("Firebase ID token")
            .description(
                "Paste a Firebase ID token. Tchalanet resolves AppUser, roles, permissions, "
                    + "tenant context, and RLS server-side.");

    var components = new Components().addSecuritySchemes("bearerAuth", bearerScheme);
    var security = new SecurityRequirement().addList("bearerAuth");

    return new OpenAPI()
        .servers(List.of(new Server().url(serverUrl)))
        .components(components)
        .addSecurityItem(security);
  }

  private static String normalizePrefix(String basePath) {
    if (basePath == null || basePath.isBlank()) return "";
    var p = basePath.startsWith("/") ? basePath : "/" + basePath;
    return p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
  }

  private static String normalizeUrl(String url) {
    // normalize accidental double slashes (keep http(s)://)
    return url.replaceAll("(?<!https?:)/{2,}", "/");
  }
}
