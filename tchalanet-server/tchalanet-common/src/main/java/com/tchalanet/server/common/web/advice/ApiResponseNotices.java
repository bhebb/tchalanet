package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.observability.TchTraceIds;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeKind;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;

/**
 * Convenience helpers for adding standardized non-blocking response notices.
 *
 * <p>Use this for partial BFF results and immediate HTTP feedback. Blocking failures should still
 * throw a stable-code exception and be rendered by {@code GlobalErrorHandler} as ProblemDetail.
 */
@UtilityClass
public class ApiResponseNotices {

  public static void info(String code, String message, String domain) {
    add(code, message, domain, NoticeSeverity.INFO, NoticeKind.INFORMATION, null, null, Map.of());
  }

  public static void info(String code, String message, String domain, NoticeSource source) {
    add(code, message, domain, NoticeSeverity.INFO, NoticeKind.INFORMATION, source, null, Map.of());
  }

  public static void warn(String code, String message, String domain) {
    add(code, message, domain, NoticeSeverity.WARN, NoticeKind.BUSINESS, null, null, Map.of());
  }

  public static void warn(String code, String message, String domain, NoticeSource source) {
    add(code, message, domain, NoticeSeverity.WARN, NoticeKind.BUSINESS, source, null, Map.of());
  }

  public static void warn(
      String code, String message, String domain, NoticeSource source, Throwable error) {
    add(code, message, domain, NoticeSeverity.WARN, NoticeKind.BUSINESS, source, error, Map.of());
  }

  public static void error(String code, String message, String domain) {
    add(code, message, domain, NoticeSeverity.ERROR, NoticeKind.BUSINESS, null, null, Map.of());
  }

  public static void error(String code, String message, String domain, NoticeSource source) {
    add(code, message, domain, NoticeSeverity.ERROR, NoticeKind.BUSINESS, source, null, Map.of());
  }

  public static void error(
      String code, String message, String domain, NoticeSource source, Throwable error) {
    add(code, message, domain, NoticeSeverity.ERROR, NoticeKind.BUSINESS, source, error, Map.of());
  }

  /** Records an optional response slice without exposing the underlying failure prose. */
  public static void degradation(
      String code,
      String domain,
      NoticeSeverity severity,
      @Nullable NoticeSource source,
      @Nullable Throwable error,
      Map<String, Object> safeParams) {
    add(code, null, domain, severity, NoticeKind.DEGRADATION, source, error, safeParams);
  }

  public static void add(
      String code,
      String message,
      String domain,
      NoticeSeverity severity,
      NoticeKind kind,
      @Nullable NoticeSource source,
      @Nullable Throwable error,
      Map<String, Object> meta) {
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

    ApiResponseContext.get()
        .addNotice(new ApiNotice(code, message, domain, severity, kind, Map.copyOf(noticeMeta)));
  }

  private static void addIfPresent(Map<String, Object> meta, String key, @Nullable String value) {
    if (value != null && !value.isBlank()) {
      meta.putIfAbsent(key, value);
    }
  }
}
