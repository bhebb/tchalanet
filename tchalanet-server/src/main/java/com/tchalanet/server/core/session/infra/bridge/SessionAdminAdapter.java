package com.tchalanet.server.core.session.infra.bridge;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.application.port.out.SessionAdminPort;
import com.tchalanet.server.core.outlet.application.port.out.SessionLookupPort;
import com.tchalanet.server.core.session.application.port.out.PosSessionWriterPort;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.session.domain.model.PosSessionStatus;
import com.tchalanet.server.core.session.infra.persistence.mapper.PosSessionMapper;
import com.tchalanet.server.core.session.infra.persistence.repository.PosSessionJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAdminAdapter implements SessionAdminPort, SessionLookupPort {

  private final PosSessionJpaRepository repo;
  private final PosSessionMapper mapper;
  private final PosSessionWriterPort writer;

  @Override
  public boolean hasOpenSessions(TenantId tenantId, OutletId outletId) {
    var found =
        repo.findByTenantIdAndOutletIdAndStatus(
            tenantId.uuid(), outletId.uuid(), PosSessionStatus.OPENED);
    return !found.isEmpty();
  }

  @Override
  public long closeAllOpenSessions(TenantId tenantId, OutletId outletId, String reason) {
    var entities =
        repo.findByTenantIdAndOutletIdAndStatus(
            tenantId.uuid(), outletId.uuid(), PosSessionStatus.OPENED);
    long count = 0;
    for (var e : entities) {
      PosSession session = mapper.toDomain(e);
      // close with zero amount for now
      PosSession updated = session.close(BigDecimal.ZERO, Instant.now());
      writer.save(updated);
      count++;
    }
    return count;
  }

  @Override
  public List<SessionId> findSessionIds(
      TenantId tenantId, OutletId outletId, Instant from, Instant to) {
    var ids =
        repo.findIdsByTenantIdAndOutletIdAndOpenedAtBetween(
            tenantId.uuid(), outletId.uuid(), from, to);
    return ids.stream().map(SessionId::of).collect(Collectors.toList());
  }
}
