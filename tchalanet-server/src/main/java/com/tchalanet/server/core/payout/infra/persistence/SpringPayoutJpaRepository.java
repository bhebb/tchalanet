package com.tchalanet.server.core.payout.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringPayoutJpaRepository extends JpaRepository<PayoutJpaEntity, UUID> {
  Optional<PayoutJpaEntity> findByTicketId(UUID ticketId);
}

