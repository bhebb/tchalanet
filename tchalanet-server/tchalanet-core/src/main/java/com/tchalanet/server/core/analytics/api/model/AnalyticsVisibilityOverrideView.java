package com.tchalanet.server.core.analytics.api.model;

import java.time.Instant;

/** Result of changing the platform-controlled visibility of an analytics scope. */
public record AnalyticsVisibilityOverrideView(
    AnalyticsTrustScope scope, boolean visible, String reason, Instant changedAt) {}
