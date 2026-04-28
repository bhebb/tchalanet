package com.tchalanet.server.core.session.infra.bridge;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.application.port.out.SessionAdminPort;
import com.tchalanet.server.core.outlet.application.port.out.SessionLookupPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import com.tchalanet.server.core.session.domain.model.SalesSessionStatus;
import com.tchalanet.server.core.session.infra.persistence.mapper.SalesSessionMapper;
import com.tchalanet.server.core.session.infra.persistence.repository.SalesSessionJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAdminAdapter implements SessionAdminPort, SessionLookupPort {

  private final SalesSessionJpaRepository repo;
  private final SalesSessionMapper mapper;
  private final SalesSessionWriterPort writer;

  @Override
  public boolean hasOpenSessions(OutletId outletId) {
    var found =
        repo.findByOutletIdAndStatus(
            outletId.value(), SalesSessionStatus.OPENED);
    return !found.isEmpty();
  }

  @Override
  public long closeAllOpenSessions(OutletId outletId, String reason) {
    var entities =
        repo.findByOutletIdAndStatus(
            outletId.value(), SalesSessionStatus.OPENED);
    long count = 0;
    for (var e : entities) {
      SalesSession session = mapper.toDomain(e);
      // close with zero amount for now
      SalesSession updated = session.close(BigDecimal.ZERO, Instant.now());
      writer.save(updated);
      count++;
    }
    return count;
  }

  @Override
  public List<SessionId> findSessionIds(
      OutletId outletId, Instant from, Instant to) {
    var ids =
        repo.findIdsByOutletIdAndOpenedAtBetween(
            outletId.value(), from, to);
    return ids.stream().map(SessionId::of).collect(Collectors.toList());
  }
}
