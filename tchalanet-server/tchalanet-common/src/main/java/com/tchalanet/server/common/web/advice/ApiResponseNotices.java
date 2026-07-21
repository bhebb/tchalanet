package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.observability.TchTraceIds;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeKind;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.api.NoticeTrace;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Convenience helpers for adding standardized non-blocking response notices.
 *
 * <p>Use this for partial BFF results and immediate HTTP feedback. Blocking failures should still
 * throw a stable-code exception and be rendered by {@code GlobalErrorHandler} as ProblemDetail.
 */
@UtilityClass
@Slf4j
public class ApiResponseNotices {

  private static final java.util.regex.Pattern FUNCTIONAL_TARGET =
      java.util.regex.Pattern.compile("[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)*");

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

  /** Records an optional response slice against a stable functional client target. */
  public static void degradation(
      String code,
      String domain,
      NoticeSeverity severity,
      @Nullable NoticeSource source,
      @Nullable Throwable error,
      Map<String, Object> safeParams,
      @Nullable String target) {
    add(code, null, domain, severity, NoticeKind.DEGRADATION, source, error, safeParams, target);
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
    add(code, message, domain, severity, kind, source, error, meta, null);
  }

  private static void add(
      String code,
      String message,
      String domain,
      NoticeSeverity severity,
      NoticeKind kind,
      @Nullable NoticeSource source,
      @Nullable Throwable error,
      Map<String, Object> meta,
      @Nullable String explicitTarget) {
    var noticeMeta = new LinkedHashMap<String, Object>();
    if (meta != null) {
      noticeMeta.putAll(meta);
    }
    noticeMeta
        .entrySet()
        .removeIf(entry -> isSensitiveKey(entry.getKey()) || !isBridgeValue(entry.getValue()));
    String target = explicitTarget == null ? targetFrom(noticeMeta) : requireTarget(explicitTarget);
    var params = publicParams(noticeMeta);
    var trace =
        new NoticeTrace(
            TchTraceIds.currentRequestId(),
            TchTraceIds.currentTraceId(),
            TchTraceIds.currentSpanId(),
            error == null ? null : UUID.randomUUID().toString());

    addIfPresent(noticeMeta, "source", source == null ? null : source.source());
    addIfPresent(noticeMeta, "service", source == null ? null : source.service());
    addIfPresent(noticeMeta, "operation", source == null ? null : source.operation());
    addIfPresent(noticeMeta, "requestId", trace.requestId());
    addIfPresent(noticeMeta, "traceId", trace.traceId());
    addIfPresent(noticeMeta, "spanId", trace.spanId());
    addIfPresent(noticeMeta, "errorId", trace.errorId());

    ApiResponseContext.get()
        .addNotice(
            new ApiNotice(
                code,
                message,
                domain,
                severity,
                kind,
                source,
                target,
                params,
                trace.errorId() == null
                        && trace.requestId() == null
                        && trace.traceId() == null
                        && trace.spanId() == null
                    ? null
                    : trace,
                Map.copyOf(noticeMeta)));
  }

  @Nullable
  private static String targetFrom(Map<String, Object> meta) {
    if (!(meta.get("target") instanceof String target)
        || !FUNCTIONAL_TARGET.matcher(target).matches()) {
      return null;
    }
    return target;
  }

  private static String requireTarget(String target) {
    if (!FUNCTIONAL_TARGET.matcher(target).matches()) {
      throw new IllegalArgumentException("Notice target must be a stable functional identifier");
    }
    return target;
  }

  private static Map<String, Object> publicParams(Map<String, Object> meta) {
    var params = new LinkedHashMap<String, Object>();
    meta.forEach(
        (key, value) -> {
          if ("target".equals(key) || "surface".equals(key) || "placement".equals(key)) {
            return;
          }
          if (isSensitiveKey(key) || !isPublicValue(value)) {
            log.warn("api_notice.param_dropped key={}", key);
            return;
          }
          params.put(key, value);
        });
    return Map.copyOf(params);
  }

  private static boolean isSensitiveKey(String key) {
    var normalized = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("password")
        || normalized.contains("token")
        || normalized.contains("secret")
        || normalized.contains("pin")
        || normalized.contains("sql")
        || normalized.contains("payload")
        || normalized.contains("provider");
  }

  private static boolean isPublicValue(Object value) {
    return value instanceof String
        || value instanceof Boolean
        || value instanceof Integer
        || value instanceof Long
        || value instanceof Double
        || value instanceof BigDecimal;
  }

  private static boolean isBridgeValue(Object value) {
    return isPublicValue(value)
        || value instanceof Map<?, ?>
        || value instanceof java.util.Collection<?>;
  }

  private static void addIfPresent(Map<String, Object> meta, String key, @Nullable String value) {
    if (value != null && !value.isBlank()) {
      meta.putIfAbsent(key, value);
    }
  }
}
