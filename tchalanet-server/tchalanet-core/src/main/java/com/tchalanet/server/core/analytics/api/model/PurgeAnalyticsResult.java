package com.tchalanet.server.core.analytics.api.model;

/** Result returned by {@code PurgeAnalyticsCommand}. */
public record PurgeAnalyticsResult(long dailyRows, long drawRows, boolean dryRun) {}
