package com.tchalanet.server.app.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.security")
public record TchSecurityProperties(
    String requiredAudience,
    UserBootstrap userBootstrap
) {

    public record UserBootstrap(
        boolean enabled,
        boolean updateLastLogin
    ) {}
}
