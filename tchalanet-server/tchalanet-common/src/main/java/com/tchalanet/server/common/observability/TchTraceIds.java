package com.tchalanet.server.common.observability;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

/**
 * Reads trace correlation IDs from MDC.
 *
 * <p>MDC keys {@code traceId} and {@code spanId} are populated automatically by
 * Micrometer Tracing once the OTel bridge is configured (B3 slice). At B1 they
 * return null gracefully, which is the correct fail-open behaviour.
 */
@UtilityClass
public class TchTraceIds {

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_BATCH_ID   = "batchId";
    public static final String MDC_TRACE_ID   = "traceId";
    public static final String MDC_SPAN_ID    = "spanId";

    public static String currentRequestId() {
        return MDC.get(MDC_REQUEST_ID);
    }

    public static String currentTraceId() {
        return MDC.get(MDC_TRACE_ID);
    }

    public static String currentSpanId() {
        return MDC.get(MDC_SPAN_ID);
    }
}
