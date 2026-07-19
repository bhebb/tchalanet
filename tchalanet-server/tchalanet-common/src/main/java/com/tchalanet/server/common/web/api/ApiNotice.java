package com.tchalanet.server.common.web.api;

import java.util.LinkedHashMap;
import java.util.Map;

/** Represents a notice or informational message in API responses. */
public record ApiNotice(
    String code,
    String message,
    String domain,
    NoticeSeverity severity,
    NoticeKind kind,
    NoticeSource source,
    String target,
    Map<String, Object> params,
    NoticeTrace trace,
    Map<String, Object> meta) {

  public ApiNotice {
    params = params == null ? Map.of() : Map.copyOf(params);
    meta = meta == null ? Map.of() : Map.copyOf(meta);
  }

  /** Compatibility constructor for producers that still provide a meta bridge. */
  public ApiNotice(
      String code,
      String message,
      String domain,
      NoticeSeverity severity,
      NoticeKind kind,
      Map<String, Object> meta) {
    this(code, message, domain, severity, kind, null, targetFrom(meta), Map.of(), traceFrom(meta), meta);
  }

  /** Compatibility constructor for message-first producers not yet migrated. */
  public ApiNotice(
      String code,
      String message,
      String domain,
      NoticeSeverity severity,
      Map<String, Object> meta) {
    this(code, message, domain, severity, NoticeKind.BUSINESS, meta);
  }

  public static ApiNotice error(String code, String message) {
    return new ApiNotice(code, message, null, NoticeSeverity.ERROR, NoticeKind.BUSINESS, Map.of());
  }

  public static ApiNotice warn(String code, String message) {
    return new ApiNotice(code, message, null, NoticeSeverity.WARN, NoticeKind.BUSINESS, Map.of());
  }

  public static ApiNotice info(String code, String message) {
    return new ApiNotice(
        code, message, null, NoticeSeverity.INFO, NoticeKind.INFORMATION, Map.of());
  }

  /**
   * Builds an informational notice without making server prose part of the client contract.
   *
   * <p>Clients render the stable {@code code} with their own locale catalog. The compatibility
   * {@code message} remains {@code null} for new producers.
   */
  public static ApiNotice information(
      String code,
      String domain,
      NoticeSource source,
      String target,
      Map<String, Object> params) {
    var safeParams = params == null ? Map.<String, Object>of() : Map.copyOf(params);
    return new ApiNotice(
        code,
        null,
        domain,
        NoticeSeverity.INFO,
        NoticeKind.INFORMATION,
        source,
        target,
        safeParams,
        null,
        legacyMeta(source, target, safeParams));
  }

  /**
   * Builds a business notice with explicit safe fields for response-payload producers.
   *
   * <p>The meta map is an additive migration bridge for consumers not yet reading the structured
   * fields directly.
   */
  public static ApiNotice business(
      String code,
      String message,
      String domain,
      NoticeSeverity severity,
      NoticeSource source,
      String target,
      Map<String, Object> params) {
    var safeParams = params == null ? Map.<String, Object>of() : Map.copyOf(params);
    return new ApiNotice(
        code,
        message,
        domain,
        severity,
        NoticeKind.BUSINESS,
        source,
        target,
        safeParams,
        null,
        legacyMeta(source, target, safeParams));
  }

  /** Builds a code-first business notice without a client-displayable server message. */
  public static ApiNotice business(
      String code,
      String domain,
      NoticeSeverity severity,
      NoticeSource source,
      String target,
      Map<String, Object> params) {
    return business(code, null, domain, severity, source, target, params);
  }

  private static Map<String, Object> legacyMeta(
      NoticeSource source, String target, Map<String, Object> params) {
    var meta = new LinkedHashMap<String, Object>(params);
    if (source != null) {
      meta.put("source", source.source());
      if (source.service() != null) meta.put("service", source.service());
      if (source.operation() != null) meta.put("operation", source.operation());
    }
    if (target != null && !target.isBlank()) meta.put("target", target);
    return Map.copyOf(meta);
  }

  private static String targetFrom(Map<String, Object> meta) {
    if (meta == null || !(meta.get("target") instanceof String target) || target.isBlank()) {
      return null;
    }
    return target;
  }

  private static NoticeTrace traceFrom(Map<String, Object> meta) {
    if (meta == null) {
      return null;
    }
    var trace =
        new NoticeTrace(
            stringValue(meta, "requestId"),
            stringValue(meta, "traceId"),
            stringValue(meta, "spanId"),
            stringValue(meta, "errorId"));
    return trace.requestId() == null
            && trace.traceId() == null
            && trace.spanId() == null
            && trace.errorId() == null
        ? null
        : trace;
  }

  private static String stringValue(Map<String, Object> meta, String key) {
    return meta.get(key) instanceof String value && !value.isBlank() ? value : null;
  }
}
