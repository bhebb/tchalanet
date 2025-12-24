package com.tchalanet.server.common.web.api;

/**
 * Represents the status of a downstream service.
 */
public record ServiceStatus(
    String service,
    ServiceHealth status,
    String message
) {}
