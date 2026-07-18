package com.tchalanet.server.common.web.error;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * Stable, client-facing identity of an API error.
 *
 * <p>The descriptor deliberately contains no human-readable message. Product copy belongs to the
 * localized clients; diagnostic prose remains private to server logs and support tooling.
 */
public record ErrorDescriptor(
    String code,
    ErrorCategory category,
    HttpStatus expectedStatus,
    ErrorRetryPolicy retryPolicy,
    Set<ErrorAudience> audiences,
    Set<ErrorParamSpec> publicParams) {

  private static final Pattern CODE_PATTERN =
      Pattern.compile("[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+");

  public ErrorDescriptor {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(expectedStatus, "expectedStatus");
    Objects.requireNonNull(retryPolicy, "retryPolicy");
    audiences = audiences == null ? Set.of() : Set.copyOf(audiences);
    publicParams = publicParams == null ? Set.of() : Set.copyOf(publicParams);
    if (!isValidCode(code)) {
      throw new IllegalArgumentException("Error code must be lowercase and namespaced");
    }
    if (publicParams.stream().map(ErrorParamSpec::name).distinct().count() != publicParams.size()) {
      throw new IllegalArgumentException("Error descriptor cannot declare duplicate public parameters");
    }
  }

  public static boolean isValidCode(String candidate) {
    return candidate != null && CODE_PATTERN.matcher(candidate).matches();
  }

  public boolean allowsParameter(String name) {
    return publicParams.stream().anyMatch(spec -> spec.name().equals(name));
  }
}
