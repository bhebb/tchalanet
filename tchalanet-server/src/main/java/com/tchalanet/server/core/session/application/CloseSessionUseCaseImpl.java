package com.tchalanet.server.core.session.application;

import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.session.application.ports.in.CloseSessionUseCase;
import com.tchalanet.server.core.session.application.ports.out.PosSessionRepositoryPort;
import com.tchalanet.server.core.session.domain.event.SessionClosedEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CloseSessionUseCaseImpl implements CloseSessionUseCase {

  private final PosSessionRepositoryPort sessionRepository;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;

  @Override
  public PosSession close(Command command) {
    PosSession existing =
        sessionRepository
            .findById(command.sessionId())
            .orElseThrow(
                () -> new IllegalStateException("PosSession not found: " + command.sessionId()));

    Instant now = Instant.now(clock);
    PosSession closed = existing.close(command.closingAmount(), now);
    PosSession saved = sessionRepository.save(closed);

    // compute totals in cents if available
    long totalStakeCents = 0L;
    long totalPayoutCents = 0L;
    try {
      BigDecimal totalStake = saved.totalStake();
      if (totalStake != null) totalStakeCents = totalStake.multiply(BigDecimal.valueOf(100)).longValue();
      BigDecimal totalPayout = saved.totalPayout();
      if (totalPayout != null) totalPayoutCents = totalPayout.multiply(BigDecimal.valueOf(100)).longValue();
    } catch (Exception _ignored) {
      // ignore, keep zeros
    }

    long netRevenueCents = totalStakeCents - totalPayoutCents;

    SessionClosedEvent event = new SessionClosedEvent(
        UUID.randomUUID(), Instant.now(clock), new TenantId(saved.tenantId()), saved.id(), saved.outletId(), saved.userId(), saved.openedAt(), saved.closedAt(), totalStakeCents, totalPayoutCents, netRevenueCents);
    domainEventPublisher.publish(event);

    return saved;
  }
}
