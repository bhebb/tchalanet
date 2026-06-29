package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.common.web.api.ServiceHealth;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Policy for a BFF slice whose failure is not allowed to abort the whole response.
 */
public record BffSlicePolicy<T>(
    String code,
    String message,
    String domain,
    NoticeSource source,
    NoticeSeverity severity,
    @Nullable ServiceHealth serviceStatus,
    @Nullable String serviceMessage,
    Supplier<T> fallback
) {

    public BffSlicePolicy {
        Objects.requireNonNull(code, "code is required");
        Objects.requireNonNull(message, "message is required");
        Objects.requireNonNull(domain, "domain is required");
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(severity, "severity is required");
        Objects.requireNonNull(fallback, "fallback is required");
    }

    public static <T> BffSlicePolicy<T> warn(
        String code,
        String message,
        String domain,
        NoticeSource source,
        Supplier<T> fallback
    ) {
        return new BffSlicePolicy<>(
            code,
            message,
            domain,
            source,
            NoticeSeverity.WARN,
            null,
            null,
            fallback
        );
    }

    public static <T> BffSlicePolicy<T> warn(
        String code,
        String message,
        String domain,
        NoticeSource source,
        T fallback
    ) {
        return warn(code, message, domain, source, () -> fallback);
    }

    public static <T> BffSlicePolicy<T> error(
        String code,
        String message,
        String domain,
        NoticeSource source,
        Supplier<T> fallback
    ) {
        return new BffSlicePolicy<>(
            code,
            message,
            domain,
            source,
            NoticeSeverity.ERROR,
            null,
            null,
            fallback
        );
    }

    public static <T> BffSlicePolicy<T> error(
        String code,
        String message,
        String domain,
        NoticeSource source,
        T fallback
    ) {
        return error(code, message, domain, source, () -> fallback);
    }

    public BffSlicePolicy<T> serviceStatus(ServiceHealth status, String message) {
        return new BffSlicePolicy<>(
            code,
            this.message,
            domain,
            source,
            severity,
            Objects.requireNonNull(status, "status is required"),
            Objects.requireNonNull(message, "message is required"),
            fallback
        );
    }
}
