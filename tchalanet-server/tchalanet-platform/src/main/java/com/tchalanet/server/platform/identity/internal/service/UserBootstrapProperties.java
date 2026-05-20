package com.tchalanet.server.platform.identity.internal.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.security.user-bootstrap")
public record UserBootstrapProperties(

    boolean enabled,
    boolean updateLastLogin

) {

}
