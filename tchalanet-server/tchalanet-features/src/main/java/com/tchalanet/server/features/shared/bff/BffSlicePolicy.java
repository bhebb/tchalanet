package com.tchalanet.server.features.shared.bff;

import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.api.ServiceHealth;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Policy for a feature-owned BFF slice whose failure does not abort the whole response. */
public record BffSlicePolicy<T>(
    String code,
    String domain,
    NoticeSource source,
    NoticeSeverity severity,
    @Nullable ServiceHealth serviceStatus,
    @Nullable String target,
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
        code, domain, source, NoticeSeverity.WARN, null, null, Map.of(), fallback);
  }

  public static <T> BffSlicePolicy<T> warn(
      String code, String domain, NoticeSource source, T fallback) {
    return warn(code, domain, source, () -> fallback);
  }

  public static <T> BffSlicePolicy<T> error(
      String code, String domain, NoticeSource source, Supplier<T> fallback) {
    return new BffSlicePolicy<>(
        code, domain, source, NoticeSeverity.ERROR, null, null, Map.of(), fallback);
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
        target,
        safeParams,
        fallback);
  }

  /** Associates the degradation with a stable feature section owned by the client. */
  public BffSlicePolicy<T> target(String target) {
    if (target == null || target.isBlank()) {
      throw new IllegalArgumentException("target is required");
    }
    return new BffSlicePolicy<>(code, domain, source, severity, serviceStatus, target, safeParams, fallback);
  }
}
