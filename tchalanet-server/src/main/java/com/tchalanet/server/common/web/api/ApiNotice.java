package com.tchalanet.server.common.web.api;

import java.util.Map;

/**
 * Represents a notice or informational message in API responses.
 */
public record ApiNotice(
    String code,
    String message,
    String domain,
    NoticeSeverity severity,
    Map<String, Object> meta
) {}
