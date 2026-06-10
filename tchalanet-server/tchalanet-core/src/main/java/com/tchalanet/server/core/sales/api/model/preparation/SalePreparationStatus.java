package com.tchalanet.server.core.sales.api.model.preparation;

/**
 * Lifecycle of a server-side sale preparation
 * (maryaj-gratis-auto-selection-v1, see DOMAIN_SALES.md §11).
 * <p>
 * DRAFT is the only mutable state. CONFIRMED / EXPIRED / CANCELLED are
 * terminal. Retention: DRAFT TTL 10 minutes; EXPIRED/CANCELLED purged after
 * 7 days; CONFIRMED kept 30 days or until reconciliation.
 */
public enum SalePreparationStatus {
    DRAFT,
    CONFIRMED,
    EXPIRED,
    CANCELLED
}
