package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.limitpolicy.api.ScopeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DrawExposureJpaRepository extends JpaRepository<DrawExposureJpaEntity, UUID> {

  @Query(
      """
      select e
      from DrawExposureJpaEntity e
      where e.drawId = :drawId
        and e.scopeType = :scopeType
        and e.scopeId = :scopeId
        and e.betType in :betTypes
        and e.deletedAt is null
  """)
  List<DrawExposureJpaEntity> findFactsForBetTypes(
      UUID drawId, ScopeType scopeType, UUID scopeId, List<BetType> betTypes);

  @Query(
      """
      select e
      from DrawExposureJpaEntity e
      where e.drawId = :drawId
        and e.scopeType = :scopeType
        and e.scopeId = :scopeId
        and e.deletedAt is null
      order by e.stakeTotal desc
  """)
  List<DrawExposureJpaEntity> topByStake(
      UUID drawId, ScopeType scopeType, UUID scopeId, Pageable pageable);
}
