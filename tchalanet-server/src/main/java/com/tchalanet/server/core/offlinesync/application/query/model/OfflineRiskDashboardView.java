package com.tchalanet.server.core.offlinesync.application.query.model;

public record OfflineRiskDashboardView(
    long reviewRequiredCount,
    long technicalRejectedCount,
    long salesRejectedCount
) {}

