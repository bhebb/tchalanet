package com.tchalanet.server.platform.archive.api.model;

/** Archive system ops counters — all field names are already valid JSON/Java identifiers. */
public record ArchiveOpsSummaryView(
    long failedRuns,
    long startedRuns,
    long completedRuns,
    long invalidObjects,
    long verifiedObjects,
    long pendingObjects) {}
