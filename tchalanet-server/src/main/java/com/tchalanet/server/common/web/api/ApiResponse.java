package com.tchalanet.server.common.web.api;

import java.util.List;
import java.util.Map;

/**
 * Standardized API response wrapper for 2xx responses.
 * Allows frontend to handle warnings, service statuses, and partial responses generically.
 */
public record ApiResponse<T>(
    ApiStatus status,
    T data,
    List<ApiNotice> notices,
    List<ServiceStatus> services
) {

    // Factory methods
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiStatus.SUCCESS, data, List.of(), List.of());
    }

    public static <T> ApiResponse<T> warn(T data, List<ApiNotice> notices) {
        return new ApiResponse<>(ApiStatus.SUCCESS_WITH_WARNINGS, data, notices, List.of());
    }

    public static <T> ApiResponse<T> pending(List<ApiNotice> notices) {
        return new ApiResponse<>(ApiStatus.PENDING, null, notices, List.of());
    }

    public static <T> ApiResponse<T> partial(T data, List<ServiceStatus> services, List<ApiNotice> notices) {
        return new ApiResponse<>(ApiStatus.PARTIAL, data, notices, services);
    }

    // Convenience methods
    public static <T> ApiResponse<T> warn(T data, ApiNotice notice) {
        return warn(data, List.of(notice));
    }

    public static ApiResponse<Void> pending(ApiNotice notice) {
        return pending(List.of(notice));
    }

    public static <T> ApiResponse<T> partial(T data, ServiceStatus service, List<ApiNotice> notices) {
        return partial(data, List.of(service), notices);
    }
}
