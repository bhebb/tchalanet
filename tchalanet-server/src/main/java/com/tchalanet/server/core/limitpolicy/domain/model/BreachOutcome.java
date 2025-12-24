package com.tchalanet.server.core.limitpolicy.domain.model;

/**
 * Enumeration of possible outcomes from limit policy evaluation.
 *
 * - ALLOW: Transaction is permitted to proceed
 * - WARN: Transaction is permitted but requires user notification
 * - BLOCK: Transaction is not permitted and must be rejected or approved
 */
public enum BreachOutcome {
    ALLOW, WARN, BLOCK
}
