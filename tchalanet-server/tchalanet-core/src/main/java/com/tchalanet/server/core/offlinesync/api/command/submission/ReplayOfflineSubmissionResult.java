package com.tchalanet.server.core.offlinesync.api.command.submission;

public record ReplayOfflineSubmissionResult(
    boolean wouldAccept,
    String code,
    String reason
) {}
