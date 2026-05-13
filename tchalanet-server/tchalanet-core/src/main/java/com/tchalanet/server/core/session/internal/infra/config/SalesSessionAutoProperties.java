package com.tchalanet.server.core.session.internal.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.sales-session.auto")
public record SalesSessionAutoProperties(
    boolean active,
    String openCron,
    String closeCron
) {}
