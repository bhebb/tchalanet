package com.tchalanet.server.core.offlinesync.internal.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param lookaheadDays number of days ahead included when listing upcoming draws on grant issue.
 * @param limit         absolute cap on the number of draws returned in a single grant response.
 */
@ConfigurationProperties(prefix = "tch.offlinesync.upcoming-draws")
public record OfflineUpcomingDrawsProperties(int lookaheadDays, int limit) {

    public OfflineUpcomingDrawsProperties {
        if (lookaheadDays <= 0) lookaheadDays = 3;
        if (limit <= 0) limit = 50;
    }
}
