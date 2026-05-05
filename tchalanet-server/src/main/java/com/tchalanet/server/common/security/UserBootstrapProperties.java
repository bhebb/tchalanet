package com.tchalanet.server.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.user-bootstrap")
public record UserBootstrapProperties(

    boolean enabled,
    boolean updateLastLogin

) {
    public UserBootstrapProperties {
        if (!enabled) enabled = true;
        if (!updateLastLogin) updateLastLogin = true;
    }
}
