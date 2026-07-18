package com.tchalanet.server.common.web.error;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable, client-facing identity of an API error.
 *
 * <p>The descriptor deliberately contains no human-readable message. Product copy belongs to the
 * localized clients; diagnostic prose remains private to server logs and support tooling.
 */
public record ErrorDescriptor(String code, ErrorCategory category, ErrorRetryPolicy retryPolicy) {

  private static final Pattern CODE_PATTERN =
      Pattern.compile("[a-z][a-z0-9]*(?:[._][a-z0-9]+)+");

  public ErrorDescriptor {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(retryPolicy, "retryPolicy");
    if (!CODE_PATTERN.matcher(code).matches()) {
      throw new IllegalArgumentException("Error code must be lowercase and namespaced");
    }
  }
}
