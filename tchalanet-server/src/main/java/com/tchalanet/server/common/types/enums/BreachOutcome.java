package com.tchalanet.server.common.types.enums;

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
