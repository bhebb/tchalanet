package com.tchalanet.server.common.context;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.context")
public record TchContextProperties(
    String publicDefaultTenantCode
) {
}
