package com.tchalanet.server.app.config.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.batch.executor")
public record BatchExecutorProperties(
    int corePoolSize,
    int maxPoolSize,
    String threadNamePrefix
) {}
