package com.tchalanet.server.common.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "tch.observability")
public record TchObservabilityProperties(
    boolean enabled,
    RequestIdProperties requestId,
    TracingProperties tracing
) {

    public record RequestIdProperties(
        boolean required,
        boolean responseHeader,
        String pattern,
        List<String> exemptPaths
    ) {}

    public record TracingProperties(
        boolean responseHeaders,
        List<String> sensitiveMessageAllowlist
    ) {}
}
