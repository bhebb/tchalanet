package com.tchalanet.server.common.web.observability;

import com.tchalanet.server.common.observability.TchObservabilityProperties;
import com.tchalanet.server.common.observability.TchTraceIds;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.tchalanet.server.common.http.TchHeaders.X_REQUEST_ID;
import static com.tchalanet.server.common.http.TchHeaders.X_SPAN_ID;
import static com.tchalanet.server.common.http.TchHeaders.X_TRACE_ID;
import static com.tchalanet.server.common.web.observability.RequiredRequestIdFilter.ATTR_REQUEST_ID;

/**
 * Sets X-Request-Id, X-Trace-Id and X-Span-Id on every response.
 *
 * <p>Fail-open: missing trace context never fails the response.
 */
public class TraceResponseHeaderFilter extends OncePerRequestFilter {

    private final TchObservabilityProperties properties;

    public TraceResponseHeaderFilter(TchObservabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
        @Nonnull HttpServletRequest req,
        @Nonnull HttpServletResponse res,
        @Nonnull FilterChain chain
    ) throws ServletException, IOException {
        try {
            chain.doFilter(req, res);
        } finally {
            if (!properties.enabled()) {
                return;
            }
            setRequestIdHeader(req, res);
            if (properties.tracing().responseHeaders()) {
                setTraceHeaders(res);
            }
        }
    }

    private static void setRequestIdHeader(HttpServletRequest req, HttpServletResponse res) {
        if (res.containsHeader(X_REQUEST_ID)) {
            return;
        }
        var requestId = (String) req.getAttribute(ATTR_REQUEST_ID);
        if (requestId == null) {
            requestId = TchTraceIds.currentRequestId();
        }
        if (requestId != null) {
            res.setHeader(X_REQUEST_ID, requestId);
        }
    }

    private static void setTraceHeaders(HttpServletResponse res) {
        try {
            var traceId = TchTraceIds.currentTraceId();
            if (traceId != null && !res.containsHeader(X_TRACE_ID)) {
                res.setHeader(X_TRACE_ID, traceId);
            }
            var spanId = TchTraceIds.currentSpanId();
            if (spanId != null && !res.containsHeader(X_SPAN_ID)) {
                res.setHeader(X_SPAN_ID, spanId);
            }
        } catch (Exception ignored) {
            // fail-open: tracing errors must never affect the response
        }
    }
}
