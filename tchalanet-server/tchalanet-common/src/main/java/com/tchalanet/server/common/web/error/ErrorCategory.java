package com.tchalanet.server.common.web.error;

/**
 * Client-safe classification for an API error.
 *
 * <p>Concrete business codes remain owned by their domain. Categories provide a deterministic
 * fallback only when a client has not yet shipped exact-code copy.
 */
public enum ErrorCategory {
  VALIDATION,
  AUTHENTICATION,
  AUTHORIZATION,
  NOT_FOUND,
  CONFLICT,
  BUSINESS_RULE,
  RATE_LIMITED,
  SERVICE_UNAVAILABLE,
  UNEXPECTED
}
