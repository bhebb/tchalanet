package com.tchalanet.server.core.limitpolicy.domain.model;

/**
 * Enumeration of scope types for limit definitions.
 *
 * Defines the aggregation scope for limit calculations:
 * - TENANT: Aggregate across the entire tenant
 * - OUTLET: Aggregate within a specific outlet
 * - ZONE: Aggregate within a geographic zone
 * - RANGE: Aggregate within a numerical range
 */
public enum ScopeType {
    TENANT, OUTLET, AGENT, ZONE, RANGE
}
