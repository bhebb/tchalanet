package com.tchalanet.server.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Small holder to expose the Spring-configured ObjectMapper to non-Spring-managed classes (eg. JPA
 * AttributeConverters). The bean is set by Spring during startup.
 */
@Component
public class ObjectMapperHolder {

  private static ObjectMapper INSTANCE;

  public ObjectMapperHolder(ObjectMapper mapper) {
    INSTANCE = mapper;
  }

  public static ObjectMapper get() {
    return INSTANCE;
  }
}
