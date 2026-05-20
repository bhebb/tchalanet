package com.tchalanet.server.common.web.api;


import java.util.Map;

/** Represents a notice or informational message in API responses. */
public record ApiNotice(
    String code,
    String message,
    String domain,
    NoticeSeverity severity,
    Map<String, Object> meta) {

  public static ApiNotice error(String code, String message) {
    return new ApiNotice(code, message, null, NoticeSeverity.ERROR, Map.of());
  }

  public static ApiNotice warn(String code, String message) {
    return new ApiNotice(code, message, null, NoticeSeverity.WARN, Map.of());
  }

  public static ApiNotice info(String code, String message) {
    return new ApiNotice(code, message, null, NoticeSeverity.INFO, Map.of());
  }
}
