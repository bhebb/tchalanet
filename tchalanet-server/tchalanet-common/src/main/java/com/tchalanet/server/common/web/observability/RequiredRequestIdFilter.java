package com.tchalanet.server.common.web.observability;

import com.tchalanet.server.common.observability.RequestId;
import com.tchalanet.server.common.observability.TchObservabilityProperties;
import com.tchalanet.server.common.observability.TchTraceIds;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.tchalanet.server.common.http.TchHeaders.X_REQUEST_ID;

/**
 * Servlet filter that enforces the X-Request-Id contract on protected endpoints.
 *
 * <p>Runs before Spring Security. On protected paths:
 * <ul>
 *   <li>missing header → 400 request_id.missing</li>
 *   <li>invalid format  → 400 request_id.invalid</li>
 *   <li>valid header    → MDC + request attribute, cleanup in finally</li>
 * </ul>
 *
 * <p>On missing/invalid, a server-generated {@code requestId} is included in the
 * ProblemDetail for log correlation, but is never returned as X-Request-Id.
 */
@Slf4j
public class RequiredRequestIdFilter extends OncePerRequestFilter {

    public static final String ATTR_REQUEST_ID = "tch.requestId";

    private final TchObservabilityProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequiredRequestIdFilter(TchObservabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
        @Nonnull HttpServletRequest req,
        @Nonnull HttpServletResponse res,
        @Nonnull FilterChain chain
    ) throws ServletException, IOException {

        if (!properties.enabled() || !properties.requestId().required()) {
            chain.doFilter(req, res);
            return;
        }

        if (isExempt(req)) {
            chain.doFilter(req, res);
            return;
        }

        var raw = req.getHeader(X_REQUEST_ID);

        if (raw == null || raw.isBlank()) {
            rejectMissing(req, res);
            return;
        }

        if (!RequestId.isValid(raw)) {
            rejectInvalid(req, res);
            return;
        }

        var requestId = raw.trim();
        MDC.put(TchTraceIds.MDC_REQUEST_ID, requestId);
        req.setAttribute(ATTR_REQUEST_ID, requestId);

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(TchTraceIds.MDC_REQUEST_ID);
        }
    }

    private boolean isExempt(HttpServletRequest req) {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        List<String> exemptPaths = properties.requestId().exemptPaths();
        if (exemptPaths == null) {
            return false;
        }
        var path = req.getRequestURI();
        return exemptPaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private void rejectMissing(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var serverRequestId = generateServerRequestId();
        log.warn("request_id.missing method={} path={} serverRequestId={}",
            req.getMethod(), req.getRequestURI(), serverRequestId);
        writeProblem(res, serverRequestId,
            "https://tchalanet/errors/request-id.missing",
            "Missing request id",
            "request_id.missing",
            "X-Request-Id header is required for this endpoint.");
    }

    private void rejectInvalid(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var serverRequestId = generateServerRequestId();
        log.warn("request_id.invalid method={} path={} serverRequestId={}",
            req.getMethod(), req.getRequestURI(), serverRequestId);
        writeProblem(res, serverRequestId,
            "https://tchalanet/errors/request-id.invalid",
            "Invalid request id",
            "request_id.invalid",
            "X-Request-Id header has an invalid format.");
    }

    private static void writeProblem(
        HttpServletResponse res,
        String serverRequestId,
        String type,
        String title,
        String code,
        String detail
    ) throws IOException {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.setContentType("application/problem+json;charset=UTF-8");
        res.setCharacterEncoding("UTF-8");

        // Values are server-controlled constants + UUID — no user input in this JSON.
        var body = """
            {"type":"%s","title":"%s","status":400,"detail":"%s","code":"%s","requestId":"%s"}"""
            .formatted(type, title, detail, code, serverRequestId);

        res.getWriter().write(body);
    }

    private static String generateServerRequestId() {
        return "srv_req_" + UUID.randomUUID().toString().replace("-", "");
    }
}
