package com.tchalanet.server.common.time;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.runtime")
public record TchRuntimeProperties(
    ZoneId zoneId
) {}
