package com.tchalanet.server.core.draw.api.query;

/** Exact KPI counts for the admin generated-draws view. */
public record DrawsSummary(
    long todayCount, long salesOpenCount, long expectedCount, long confirmedCount) {}
