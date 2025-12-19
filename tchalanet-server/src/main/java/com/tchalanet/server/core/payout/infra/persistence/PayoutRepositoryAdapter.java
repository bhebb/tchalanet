package com.tchalanet.server.core.payout.infra.persistence;

import com.tchalanet.server.core.payout.port.out.PayoutRepositoryPort;
import com.tchalanet.server.core.payout.domain.model.Payout;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayoutRepositoryAdapter implements PayoutRepositoryPort {

  private final SpringPayoutJpaRepository jpaRepo;

  @Override
  public Payout save(Payout payout) {
    var e = toEntity(payout);
    var saved = jpaRepo.save(e);
    return toDomain(saved);
  }

  @Override
  public Optional<Payout> findByTicketId(UUID ticketId) {
    return jpaRepo.findByTicketId(ticketId).map(this::toDomain);
  }

  @Override
  public Optional<Payout> findById(UUID payoutId) {
    return jpaRepo.findById(payoutId).map(this::toDomain);
  }

  private Payout toDomain(PayoutJpaEntity e) {
    return Payout.load(
        e.getId(),
        e.getTenantId(),
        e.getTicketId(),
        e.getAmount(),
        com.tchalanet.server.core.payout.domain.model.PayoutStatus.valueOf(e.getStatus()),
        e.getCreatedAt(),
        e.getApprovedAt(),
        e.getPaidAt(),
        e.getVersion() == null ? 0L : e.getVersion());
  }

  private PayoutJpaEntity toEntity(Payout p) {
    var e = new PayoutJpaEntity();
    e.setId(p.getId());
    e.setTenantId(p.getTenantId());
    e.setTicketId(p.getTicketId());
    e.setAmount(p.getAmount());
    e.setStatus(p.getStatus().name());
    e.setCreatedAt(p.getCreatedAt());
    e.setApprovedAt(p.getApprovedAt());
    e.setPaidAt(p.getPaidAt());
    e.setVersion(p.getVersion());
    return e;
  }
}
