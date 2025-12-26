package com.tchalanet.server.core.limitpolicy.infra.persistence.repository;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.ScopeType;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.DrawExposureJpaEntity;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DrawExposureJpaRepository extends JpaRepository<DrawExposureJpaEntity, DrawExposureJpaEntity.DrawExposureId> {

  @Query("SELECT e FROM DrawExposureJpaEntity e WHERE e.tenantId = :tenantId AND e.drawId = :drawId AND e.scopeType = :scopeType AND e.scopeId = :scopeId AND e.betType = :betType AND e.selectionKey = :selectionKey")
  DrawExposureJpaEntity findByKey(UUID tenantId, UUID drawId, ScopeType scopeType, UUID scopeId, BetType betType, String selectionKey);

  @Query("SELECT COALESCE(SUM(e.stakeTotal), 0) FROM DrawExposureJpaEntity e WHERE e.tenantId = :tenantId AND e.drawId = :drawId AND e.scopeType = :scopeType AND e.scopeId = :scopeId AND e.betType IS NULL AND e.selectionKey IS NULL")
  BigDecimal sumStakeForDraw(UUID tenantId, UUID drawId, ScopeType scopeType, UUID scopeId);

  @Modifying
  @Transactional
  @Query(value = "SELECT increment_draw_exposure(:tenantId, :drawId, :scopeType, :scopeId, :betType, :selectionKey, :stakeDelta, :salesDelta, :payoutDelta)", nativeQuery = true)
  void incrementExposure(UUID tenantId, UUID drawId, ScopeType scopeType, UUID scopeId, BetType betType, String selectionKey, BigDecimal stakeDelta, long salesDelta, BigDecimal payoutDelta);
}
