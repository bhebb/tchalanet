package com.tchalanet.server.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.Scopes;
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

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/tchalanet}")
  private String issuerUri;

  @Bean
  public OpenAPI customOpenAPI() {
    String prefix = basePath;
    if (!prefix.startsWith("/")) prefix = "/" + prefix;
    // avoid trailing slash before version
    if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
    String url = apiBaseUrl + prefix + "/" + apiVersion;
    // normalize double slashes
    url = url.replaceAll("(?<!https?:)/{2,}", "/");

    // Compose OAuth2 endpoints from issuer (Keycloak)
    String authUrl = issuerUri;
    if (authUrl.endsWith("/")) authUrl = authUrl.substring(0, authUrl.length() - 1);
    String authorizationEndpoint = authUrl + "/protocol/openid-connect/auth";
    String tokenEndpoint = authUrl + "/protocol/openid-connect/token";

    // build scopes object (OpenAPI Scopes)
    Scopes scopes = new Scopes();
    scopes.addString("openid", "OpenID scope");
    scopes.addString("profile", "Profile scope");
    scopes.addString("email", "Email scope");

    OAuthFlow authCodeFlow =
        new OAuthFlow()
            .authorizationUrl(authorizationEndpoint)
            .tokenUrl(tokenEndpoint)
            .scopes(scopes);

    var flows = new OAuthFlows();
    flows.authorizationCode(authCodeFlow);

    var oauth2Scheme =
        new SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(flows).name("oauth2");

    var components = new Components().addSecuritySchemes("oauth2", oauth2Scheme);

    var securityRequirement = new SecurityRequirement().addList("oauth2", List.of("openid", "profile", "email"));

    return new OpenAPI().servers(List.of(new Server().url(url))).components(components).addSecurityItem(securityRequirement);
  }
}
