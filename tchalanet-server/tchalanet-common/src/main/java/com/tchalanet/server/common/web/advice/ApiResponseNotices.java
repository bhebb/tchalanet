package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.observability.TchTraceIds;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Convenience helpers for adding standardized non-blocking response notices.
 *
 * <p>Use this for partial BFF results and immediate HTTP feedback. Blocking failures should still
 * throw a stable-code exception and be rendered by {@code GlobalErrorHandler} as ProblemDetail.
 */
@UtilityClass
public class ApiResponseNotices {

    public static void info(String code, String message, String domain) {
        add(code, message, domain, NoticeSeverity.INFO, null, null, Map.of());
    }

    public static void info(String code, String message, String domain, NoticeSource source) {
        add(code, message, domain, NoticeSeverity.INFO, source, null, Map.of());
    }

    public static void warn(String code, String message, String domain) {
        add(code, message, domain, NoticeSeverity.WARN, null, null, Map.of());
    }

    public static void warn(String code, String message, String domain, NoticeSource source) {
        add(code, message, domain, NoticeSeverity.WARN, source, null, Map.of());
    }

    public static void warn(
        String code,
        String message,
        String domain,
        NoticeSource source,
        Throwable error
    ) {
        add(code, message, domain, NoticeSeverity.WARN, source, error, Map.of());
    }

    public static void error(String code, String message, String domain) {
        add(code, message, domain, NoticeSeverity.ERROR, null, null, Map.of());
    }

    public static void error(String code, String message, String domain, NoticeSource source) {
        add(code, message, domain, NoticeSeverity.ERROR, source, null, Map.of());
    }

    public static void error(
        String code,
        String message,
        String domain,
        NoticeSource source,
        Throwable error
    ) {
        add(code, message, domain, NoticeSeverity.ERROR, source, error, Map.of());
    }

    public static void add(
        String code,
        String message,
        String domain,
        NoticeSeverity severity,
        @Nullable NoticeSource source,
        @Nullable Throwable error,
        Map<String, Object> meta
    ) {
        var noticeMeta = new LinkedHashMap<String, Object>();
        if (meta != null) {
            noticeMeta.putAll(meta);
        }
        addIfPresent(noticeMeta, "source", source == null ? null : source.source());
        addIfPresent(noticeMeta, "service", source == null ? null : source.service());
        addIfPresent(noticeMeta, "operation", source == null ? null : source.operation());
        addIfPresent(noticeMeta, "requestId", TchTraceIds.currentRequestId());
        addIfPresent(noticeMeta, "traceId", TchTraceIds.currentTraceId());
        addIfPresent(noticeMeta, "spanId", TchTraceIds.currentSpanId());
        if (error != null) {
            noticeMeta.putIfAbsent("errorId", UUID.randomUUID().toString());
        }

        ApiResponseContext.get().addNotice(new ApiNotice(
            code,
            message,
            domain,
            severity,
            Map.copyOf(noticeMeta)
        ));
    }

    private static void addIfPresent(Map<String, Object> meta, String key, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            meta.putIfAbsent(key, value);
        }
    }
}
