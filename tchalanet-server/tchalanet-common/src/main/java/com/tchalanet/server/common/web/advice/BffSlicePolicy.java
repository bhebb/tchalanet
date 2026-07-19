package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.api.ServiceHealth;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Policy for a BFF slice whose failure is not allowed to abort the whole response. */
public record BffSlicePolicy<T>(
    String code,
    String domain,
    NoticeSource source,
    NoticeSeverity severity,
    @Nullable ServiceHealth serviceStatus,
    Map<String, Object> safeParams,
    Supplier<T> fallback) {

  public BffSlicePolicy {
    Objects.requireNonNull(code, "code is required");
    Objects.requireNonNull(domain, "domain is required");
    Objects.requireNonNull(source, "source is required");
    Objects.requireNonNull(severity, "severity is required");
    safeParams = safeParams == null ? Map.of() : Map.copyOf(safeParams);
    Objects.requireNonNull(fallback, "fallback is required");
  }

  public static <T> BffSlicePolicy<T> warn(
      String code, String domain, NoticeSource source, Supplier<T> fallback) {
    return new BffSlicePolicy<>(
        code, domain, source, NoticeSeverity.WARN, null, Map.of(), fallback);
  }

  public static <T> BffSlicePolicy<T> warn(
      String code, String domain, NoticeSource source, T fallback) {
    return warn(code, domain, source, () -> fallback);
  }

  /**
   * @deprecated Use the code-first overload; messages are not a client-display contract.
   */
  @Deprecated(forRemoval = false)
  public static <T> BffSlicePolicy<T> warn(
      String code,
      String ignoredMessage,
      String domain,
      NoticeSource source,
      Supplier<T> fallback) {
    return warn(code, domain, source, fallback);
  }

  /**
   * @deprecated Use the code-first overload; messages are not a client-display contract.
   */
  @Deprecated(forRemoval = false)
  public static <T> BffSlicePolicy<T> warn(
      String code, String ignoredMessage, String domain, NoticeSource source, T fallback) {
    return warn(code, domain, source, fallback);
  }

  public static <T> BffSlicePolicy<T> error(
      String code, String domain, NoticeSource source, Supplier<T> fallback) {
    return new BffSlicePolicy<>(
        code, domain, source, NoticeSeverity.ERROR, null, Map.of(), fallback);
  }

  public static <T> BffSlicePolicy<T> error(
      String code, String domain, NoticeSource source, T fallback) {
    return error(code, domain, source, () -> fallback);
  }

  public BffSlicePolicy<T> serviceStatus(ServiceHealth status) {
    return new BffSlicePolicy<>(
        code,
        domain,
        source,
        severity,
        Objects.requireNonNull(status, "status is required"),
        safeParams,
        fallback);
  }

  /** Associates the degradation with a stable feature section owned by the client. */
  public BffSlicePolicy<T> target(String target) {
    if (target == null || target.isBlank()) {
      throw new IllegalArgumentException("target is required");
    }
    var params = new java.util.LinkedHashMap<>(safeParams);
    params.put("surface", "section");
    params.put("placement", "top");
    params.put("target", target);
    return new BffSlicePolicy<>(code, domain, source, severity, serviceStatus, params, fallback);
  }

  /**
   * @deprecated Service-status prose is diagnostic-only and must not be returned to clients.
   */
  @Deprecated(forRemoval = false)
  public BffSlicePolicy<T> serviceStatus(ServiceHealth status, String ignoredMessage) {
    return serviceStatus(status);
  }
}
