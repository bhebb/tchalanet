package com.tchalanet.server.common.web.error;

/** Whether a client may offer a retry without changing user input. */
public enum ErrorRetryPolicy {
  NEVER,
  AFTER_USER_ACTION,
  SAFE_RETRY
}
