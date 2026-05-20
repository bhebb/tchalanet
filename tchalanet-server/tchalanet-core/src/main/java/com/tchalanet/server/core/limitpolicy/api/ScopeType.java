package com.tchalanet.server.core.limitpolicy.api;

/**
 * Enumeration of scope types for limit definitions.
 *
 * <p>Defines the aggregation scope for limit calculations: - TENANT: Aggregate across the entire
 * tenant - OUTLET: Aggregate within a specific outlet - ZONE: Aggregate within a geographic zone -
 * RANGE: Aggregate within a numerical range
 */
public enum ScopeType {
    TENANT,
    OUTLET,
    AGENT,
    ZONE,
    TERMINAL,
    DRAW_CHANNEL,
    RANGE
}
