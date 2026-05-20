package com.tchalanet.server.core.pagemodel.internal.domain.exception;

import java.util.List;

/**
 * Thrown when a page model JSON fails validation against its template JSON Schema.
 * value = SCHEMA_VIOLATION
 */
public class PageModelSchemaViolationException extends PageModelDomainException {

  public record Violation(String path, String message) {}

  private final List<Violation> violations;

  public PageModelSchemaViolationException(String logicalId, List<Violation> violations) {
    super("PageModel schema validation failed for logicalId=%s : %d violation(s)".formatted(logicalId, violations.size()));
    this.violations = List.copyOf(violations);
  }

  public List<Violation> getViolations() {
    return violations;
  }
}

