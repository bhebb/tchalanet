package com.tchalanet.server.core.payout.infra.persistence;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.payout.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.domain.model.Payout;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayoutRepositoryAdapter implements PayoutReaderPort {

  private final SpringPayoutJpaRepository jpaRepo;

  @Override
  public Optional<Payout> findByTicketId( TicketId ticketId) {
    return jpaRepo.findByTicketId(ticketId.uuid()).map(this::toDomain);
  }

  @Override
  public Optional<Payout> findById( PayoutId payoutId) {
    return jpaRepo.findById(payoutId.uuid()).map(this::toDomain);
  }

  private Payout toDomain(PayoutJpaEntity e) {
    long amountCents = e.getAmountCents() == null ? 0L : e.getAmountCents();
    String currency = e.getCurrency() == null ? "HTG" : e.getCurrency();

    return Payout.load(
        PayoutId.of(e.getId()),
        TenantId.of(e.getTenantId()),
        com.tchalanet.server.common.types.id.TicketId.of(e.getTicketId()),
        amountCents,
        currency,
        com.tchalanet.server.core.payout.domain.model.PayoutStatus.valueOf(e.getStatus()),
        e.getCreatedAt(),
        e.getApprovedAt(),
        e.getPaidAt(),
        e.getRejectedReason(),
        e.getRejectedAt(),
        e.getVersion());
  }

  private PayoutJpaEntity toEntity(Payout p) {
    var e = new PayoutJpaEntity();
    if (p.getId() != null) e.setId(p.getId().uuid());
    if (p.getTenantId() != null) e.setTenantId(p.getTenantId().uuid());
    if (p.getTicketId() != null) e.setTicketId(p.getTicketId().uuid());
    e.setAmountCents(p.getAmountCents());
    e.setCurrency(p.getCurrency());
    e.setStatus(p.getStatus().name());
    e.setCreatedAt(p.getCreatedAt());
    e.setApprovedAt(p.getApprovedAt());
    e.setPaidAt(p.getPaidAt());
    e.setRejectedAt(p.getRejectedAt());
    e.setRejectedReason(p.getRejectedReason());
    return e;
  }
}
