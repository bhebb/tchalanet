package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.web.api.ServiceStatus;
import lombok.experimental.UtilityClass;

/**
 * Deterministic helper for BFF endpoints that aggregate required and optional slices.
 *
 * <p>Required slices preserve normal exception flow. Optional slices add a standardized notice and
 * return their fallback value so {@code ApiResponseBodyAdvice} can publish one partial-success
 * response.
 */
@UtilityClass
public class BffSlices {

    public static <T> T required(SliceSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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
        ApiResponseNotices.add(
            policy.code(),
            policy.message(),
            policy.domain(),
            policy.severity(),
            policy.source(),
            ex,
            java.util.Map.of()
        );

        if (policy.serviceStatus() != null && policy.source().service() != null) {
            ApiResponseContext.get().addServiceStatus(new ServiceStatus(
                policy.source().service(),
                policy.serviceStatus(),
                policy.serviceMessage()
            ));
        }
    }

    @FunctionalInterface
    public interface SliceSupplier<T> {
        T get() throws Exception;
    }
}
