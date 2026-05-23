package com.tchalanet.server.core.promotion.internal.infra.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PromotionRuleRepository extends JpaRepository<PromotionRuleJpaEntity, UUID> {

  @Query("""
    select r from PromotionRuleJpaEntity r
    where r.active = true
      and r.archivedAt is null
      and (r.startsAt is null or r.startsAt <= :saleAt)
      and (r.endsAt is null or r.endsAt > :saleAt)
    order by r.priority asc
  """)
  List<PromotionRuleJpaEntity> findActiveCandidates(Instant saleAt);
}
