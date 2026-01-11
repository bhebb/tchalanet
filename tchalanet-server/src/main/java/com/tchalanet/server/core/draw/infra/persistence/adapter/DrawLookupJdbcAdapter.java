package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawLookupJdbcRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawLookupJdbcAdapter implements DrawLookupPort {

  private final DrawLookupJdbcRepository repo;

  @Override
  public Optional<java.util.UUID> findDrawId(
      TenantId tenantId, LocalDate drawDate, String slotKey) {
    if (tenantId == null || drawDate == null || slotKey == null || slotKey.isBlank())
      return Optional.empty();
    UUID id = repo.findDrawId(tenantId.value(), drawDate, slotKey);
    return Optional.ofNullable(id);
  }

  @Override
  public Optional<UUID> findDrawIdBySlotId(
      TenantId tenantId, LocalDate drawDate, UUID resultSlotId) {
    if (tenantId == null || drawDate == null || resultSlotId == null) return Optional.empty();
    UUID id = repo.findDrawIdBySlotId(tenantId.value(), drawDate, resultSlotId);
    return Optional.ofNullable(id);
  }
}
