package com.tchalanet.server.common.web.api;

import lombok.experimental.UtilityClass;

/**
 * Shared API-facing error codes owned by the common layer.
 *
 * <p>Domain, platform, and feature modules own their own namespaced codes close to their public API.
 */
@UtilityClass
public class CommonErrorCodes {

    public static final String ACCESS_DENIED = "access.denied";
    public static final String BUSINESS_RULE_VIOLATION = "business_rule.violation";
    public static final String INTERNAL_UNEXPECTED = "internal.unexpected";
    public static final String REQUEST_MISSING_PARAMETER = "request.missing_parameter";
    public static final String REQUEST_NOT_READABLE = "request.not_readable";
    public static final String REQUEST_TYPE_MISMATCH = "request.type_mismatch";
    public static final String VALIDATION_CONSTRAINT_VIOLATION = "validation.constraint_violation";
    public static final String VALIDATION_FAILED = "validation.failed";
}
