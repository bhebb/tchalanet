package com.tchalanet.server.common.config;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Holder exposing the Spring-managed Jackson 3 {@link JsonMapper} to non-Spring-managed classes
 * (e.g., JPA {@code AttributeConverter}s). The bean is initialized by Spring during startup.
 *
 * <p>Renamed semantically from {@code ObjectMapperHolder} → {@code JsonMapperHolder} as part of the
 * Jackson 2 → 3 migration. The class name is kept for binary compatibility during the migration
 * window; a follow-up rename PR is planned.
 */
@Component
public class ObjectMapperHolder {

  private static JsonMapper INSTANCE;

  public ObjectMapperHolder(JsonMapper mapper) {
    INSTANCE = mapper;
  }

  /** Returns the singleton, Spring-managed Jackson 3 {@link JsonMapper}. */
  public static JsonMapper get() {
    return INSTANCE;
  }
}
