package com.tchalanet.server.core.offlinesync.api.query;

public record OfflineRiskDashboardView(
    long reviewRequiredCount,
    long technicalRejectedCount,
    long salesRejectedCount
) {}

