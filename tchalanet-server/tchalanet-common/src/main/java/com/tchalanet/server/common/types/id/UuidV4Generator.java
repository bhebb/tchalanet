package com.tchalanet.server.common.types.id;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Production UUID v4 generator using {@link UUID#randomUUID()}.
 */
@Component
public class UuidV4Generator implements IdGenerator {

  @Override
  public UUID newUuid() {
    return UUID.randomUUID();
  }
}
