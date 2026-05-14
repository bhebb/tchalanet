package com.tchalanet.server.core.limitpolicy;

/**
 * Enumeration of possible outcomes from limit policy evaluation.
 *
 * <p>- ALLOW: Transaction is permitted to proceed - WARN: Transaction is permitted but requires
 * user notification - BLOCK: Transaction is not permitted and must be rejected or approved
 */
public enum BreachOutcome {
  ALLOW,
  WARN,
    REQUIRE_APPROVAL, BLOCK
}
