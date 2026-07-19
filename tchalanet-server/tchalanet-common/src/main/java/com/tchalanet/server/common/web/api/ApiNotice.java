package com.tchalanet.server.common.web.api;

import java.util.Map;

/** Represents a notice or informational message in API responses. */
public record ApiNotice(
    String code,
    String message,
    String domain,
    NoticeSeverity severity,
    NoticeKind kind,
    Map<String, Object> meta) {

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
}
