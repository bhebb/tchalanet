package com.tchalanet.server.core.offlinesync.api.query.dashboard;

/** Operational dashboard counters: active grants + pending review + recent rejections. */
public record OfflineDashboardView(
    int activeGrantCount,
    int pendingReviewSubmissionCount,
    int last24hAcceptedSubmissionCount,
    int last24hRejectedSubmissionCount,
    int stuckSubmissionCount
) {}
