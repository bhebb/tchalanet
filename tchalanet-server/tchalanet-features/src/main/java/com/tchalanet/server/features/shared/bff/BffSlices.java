package com.tchalanet.server.features.shared.bff;

import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.advice.ApiResponseNotices;
import com.tchalanet.server.common.web.api.ServiceStatus;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Feature composition support for required and optional BFF slices.
 *
 * <p>Required slices preserve their exception flow. Optional slices retain primary data, publish a
 * degradation notice, and return their explicit fallback. The helper deliberately does not catch
 * JVM {@link Error}s.
 */
@UtilityClass
@Slf4j
public class BffSlices {

  public static <T> T required(SliceSupplier<T> supplier) {
    return supplier.get();
  }

  public static <T> T optional(BffSlicePolicy<T> policy, SliceSupplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RuntimeException ex) {
      addFailure(policy, ex);
      return policy.fallback().get();
    }
  }

  private static void addFailure(BffSlicePolicy<?> policy, RuntimeException ex) {
    log.warn(
        "bff_slice.degraded code={} domain={} source={} operation={}",
        policy.code(),
        policy.domain(),
        policy.source().source(),
        policy.source().operation(),
        ex);
    ApiResponseNotices.degradation(
        policy.code(),
        policy.domain(),
        policy.severity(),
        policy.source(),
        ex,
        policy.safeParams(),
        policy.target());

    if (policy.serviceStatus() != null && policy.source().service() != null) {
      ApiResponseContext.get()
          .addServiceStatus(
              new ServiceStatus(policy.source().service(), policy.serviceStatus(), null));
    }
  }

  @FunctionalInterface
  public interface SliceSupplier<T> {
    T get();
  }
}
