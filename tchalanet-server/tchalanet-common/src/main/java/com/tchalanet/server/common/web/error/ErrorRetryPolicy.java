package com.tchalanet.server.common.web.error;

/**
 * Recovery action a client may offer for a stable error.
 *
 * <p>Only {@link #RETRY_SAME_INTENT} permits repeating the exact idempotent request. It must never
 * be used to make a client automatically repeat a non-idempotent mutation.
 */
public enum ErrorRetryPolicy {
  NEVER,
  AFTER_USER_ACTION,
  AFTER_REAUTH,
  AFTER_DELAY,
  RETRY_SAME_INTENT;

  public boolean retryable() {
    return this != NEVER;
  }
}
