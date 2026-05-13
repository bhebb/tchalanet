package com.tchalanet.server.app.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
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

  @Value(
      "${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/tchalanet}")
  private String issuerUri;

  @Bean
  public OpenAPI customOpenAPI() {
    var prefix = normalizePrefix(basePath);
    var serverUrl = normalizeUrl(apiBaseUrl + prefix + "/" + apiVersion);

    // --- Keycloak endpoints (issuer already includes /realms/<realm>)
    var issuer =
        issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri;
    var authorizationEndpoint = issuer + "/protocol/openid-connect/auth";
    var tokenEndpoint = issuer + "/protocol/openid-connect/token";

    // --- OAuth2 (authorization code) scopes
    var oauthScopes =
        new Scopes()
            .addString("basic", "OpenID scope")
            .addString("openid", "OpenID scope")
            .addString("profile", "Profile scope")
            .addString("email", "Email scope")
            // match what you actually use in tokens
            .addString("roles", "Roles scope")
            .addString("tch", "Tchalanet API scope");

    var authCodeFlow =
        new OAuthFlow()
            .authorizationUrl(authorizationEndpoint)
            .tokenUrl(tokenEndpoint)
            .scopes(oauthScopes);

    var flows = new OAuthFlows().authorizationCode(authCodeFlow);

    var oauth2Scheme =
        new SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(flows).name("oauth2");

    // --- Bearer JWT (recommended for Swagger Try-it-out reliability)
    var bearerScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .name("bearerAuth");

    var components =
        new Components()
            .addSecuritySchemes("bearerAuth", bearerScheme)
            .addSecuritySchemes("oauth2", oauth2Scheme);

    // Apply security globally:
    // 1) bearerAuth (paste token)
    // 2) oauth2 (login flow)
    var security =
        new SecurityRequirement()
            .addList("bearerAuth")
            .addList("oauth2", List.of("openid", "profile", "email", "roles", "tch"));

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
