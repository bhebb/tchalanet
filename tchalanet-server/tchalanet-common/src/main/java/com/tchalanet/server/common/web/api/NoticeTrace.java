package com.tchalanet.server.common.web.api;

import jakarta.annotation.Nullable;

/** Correlation identifiers attached to a single non-blocking API notice. */
public record NoticeTrace(
    @Nullable String requestId,
    @Nullable String traceId,
    @Nullable String spanId,
    @Nullable String errorId) {}
