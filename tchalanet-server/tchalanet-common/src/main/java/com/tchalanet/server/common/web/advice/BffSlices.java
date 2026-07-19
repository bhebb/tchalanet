package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.web.api.ServiceStatus;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Deterministic helper for BFF endpoints that aggregate required and optional slices.
 *
 * <p>Required slices preserve normal exception flow. Optional slices add a standardized notice and
 * return their fallback value so {@code ApiResponseBodyAdvice} can publish one partial-success
 * response.
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
    } catch (RuntimeException | Error ex) {
      addFailure(policy, ex);
      return policy.fallback().get();
    } catch (Exception ex) {
      addFailure(policy, ex);
      return policy.fallback().get();
    }
  }

  private static void addFailure(BffSlicePolicy<?> policy, Throwable ex) {
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
        policy.safeParams());

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
