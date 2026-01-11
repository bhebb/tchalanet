package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DrawLookupPort {
  Optional<UUID> findDrawId(TenantId tenantId, LocalDate drawDate, String slotKey);

  Optional<UUID> findDrawIdBySlotId(TenantId tenantId, LocalDate drawDate, UUID resultSlotId);
}
