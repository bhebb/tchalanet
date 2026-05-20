package com.tchalanet.server.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String version,
    String apiVersion,
    String basePath,
    URI apiBaseUrl,
    URI authUrl,
    Cors cors
) {

    public record Cors(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        boolean allowCredentials
    ) {}
}
