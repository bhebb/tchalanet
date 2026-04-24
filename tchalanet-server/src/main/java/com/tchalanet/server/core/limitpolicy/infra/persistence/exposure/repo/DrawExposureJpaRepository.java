package com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.repo;

import com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.entity.DrawExposureJpaEntity;
import com.tchalanet.server.common.types.enums.ScopeType;
import com.tchalanet.server.common.types.enums.BetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DrawExposureJpaRepository extends JpaRepository<DrawExposureJpaEntity, UUID> {

  @Query("""
      select e
      from DrawExposureJpaEntity e
      where e.drawId = :drawId
        and e.scopeType = :scopeType
        and e.scopeId = :scopeId
        and e.betType in :betTypes
        and e.deletedAt is null
  """)
  List<DrawExposureJpaEntity> findFactsForBetTypes(UUID drawId, ScopeType scopeType, UUID scopeId, List<BetType> betTypes);

  @Query("""
      select e
      from DrawExposureJpaEntity e
      where e.drawId = :drawId
        and e.scopeType = :scopeType
        and e.scopeId = :scopeId
        and e.deletedAt is null
      order by e.stakeTotal desc
  """)
  List<DrawExposureJpaEntity> topByStake(UUID drawId, ScopeType scopeType, UUID scopeId, Pageable pageable);

  @Query("""
      select e
      from DrawExposureJpaEntity e
      where e.drawId = :drawId
        and e.scopeType = :scopeType
        and e.scopeId = :scopeId
        and e.deletedAt is null
      order by e.potentialPayoutTotal desc
  """)
  List<DrawExposureJpaEntity> topByPayout(UUID drawId, ScopeType scopeType, UUID scopeId, Pageable pageable);
}
