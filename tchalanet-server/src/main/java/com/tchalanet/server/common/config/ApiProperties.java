package com.tchalanet.server.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record ApiProperties(
    String version,
    String basePath,
    String apiVersion,
    String apiBaseUrl,
    String authUrl,
    String defaultTenant) {}
