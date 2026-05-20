package com.tchalanet.server.common.types.id;

import java.util.UUID;

/**
 * Typed identifier for PageModelTemplate.
 */
public record PageModelTemplateId(UUID value) {
  public PageModelTemplateId {
    if (value == null) throw new IllegalArgumentException("PageModelTemplateId.value is null");
  }

  public static PageModelTemplateId of(UUID value) {
    return new PageModelTemplateId(value);
  }

  public static PageModelTemplateId nullableOf(UUID raw) {
    return raw == null ? null : new PageModelTemplateId(raw);
  }

  public static PageModelTemplateId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("PageModelTemplateId is required");
    return new PageModelTemplateId(UUID.fromString(raw));
  }
}
