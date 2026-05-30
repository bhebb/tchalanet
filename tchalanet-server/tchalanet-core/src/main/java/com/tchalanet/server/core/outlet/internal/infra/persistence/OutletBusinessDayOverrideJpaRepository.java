package com.tchalanet.server.core.outlet.internal.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutletBusinessDayOverrideJpaRepository
    extends JpaRepository<OutletBusinessDayOverrideJpaEntity, UUID> {

  Optional<OutletBusinessDayOverrideJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  Optional<OutletBusinessDayOverrideJpaEntity>
      findByTenantIdAndOutletIdAndBusinessDateAndDeletedAtIsNull(
          UUID tenantId, UUID outletId, LocalDate businessDate);

  List<OutletBusinessDayOverrideJpaEntity>
      findByTenantIdAndOutletIdAndBusinessDateBetweenAndDeletedAtIsNullOrderByBusinessDateAsc(
          UUID tenantId, UUID outletId, LocalDate from, LocalDate to);
}
