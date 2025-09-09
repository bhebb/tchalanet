package com.tchalanet.server.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record ApiProperties(String basePath, String apiVersion, String defaultTenant) {}
