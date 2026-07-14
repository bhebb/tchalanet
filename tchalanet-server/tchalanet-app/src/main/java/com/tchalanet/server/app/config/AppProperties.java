package com.tchalanet.server.app.config;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String version, String apiVersion, String basePath, URI apiBaseUrl, URI authUrl, Cors cors) {

  public record Cors(
      List<String> allowedOrigins,
      List<String> allowedMethods,
      List<String> allowedHeaders,
      List<String> exposedHeaders,
      boolean allowCredentials) {}
}
