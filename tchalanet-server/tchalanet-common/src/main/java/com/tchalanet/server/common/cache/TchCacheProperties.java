package com.tchalanet.server.common.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.cache")
public record TchCacheProperties(
    Redis redis
) {

    public record Redis(
        boolean enabled
    ) {}
}
