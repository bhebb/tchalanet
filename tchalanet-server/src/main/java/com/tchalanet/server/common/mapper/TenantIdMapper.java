package com.tchalanet.server.common.mapper;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Helper mapper for converting between UUID and TenantId wrapper used across MapStruct mappers.
 */
@Component
public class TenantIdMapper {

  public UUID mapFromTenantId(TenantId id) {
    return id == null ? null : id.uuid();
  }

  public TenantId mapToTenantId(UUID id) {
    return id == null ? null : TenantId.of(id);
  }
}

