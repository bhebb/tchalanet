package com.tchalanet.server.common.mapper;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Helper mapper for audit-related fields (createdAt, updatedAt, deletedAt, deletedBy, version).
 * MapStruct mappers can include this via `uses = {CommonAuditMapper.class}` to reuse conversions or
 * to explicitly ignore these fields when mapping domain -> entity.
 */
@Component
public class CommonAuditMapper {

  // Identity passthroughs — useful when types match but MapStruct needs an explicit conversion
  // method
  public Instant mapInstant(Instant value) {
    return value;
  }

  public UUID mapUuid(UUID value) {
    return value;
  }
}
