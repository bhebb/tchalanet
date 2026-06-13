package com.tchalanet.server.common.web.api;

/**
 * Diagnostic trace identifiers attached to successful API responses.
 *
 * <p>Fields are null when the corresponding context is unavailable.
 * Serialization skips null fields — callers must tolerate absent trace info.
 */
public record ApiTraceInfo(String requestId, String traceId, String spanId) {

    public static ApiTraceInfo of(String requestId, String traceId, String spanId) {
        return new ApiTraceInfo(requestId, traceId, spanId);
    }

    public boolean hasAny() {
        return requestId != null || traceId != null || spanId != null;
    }
}
