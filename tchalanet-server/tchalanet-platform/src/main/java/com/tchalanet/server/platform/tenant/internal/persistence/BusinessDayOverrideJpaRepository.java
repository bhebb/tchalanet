package com.tchalanet.server.platform.tenant.internal.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessDayOverrideJpaRepository
    extends JpaRepository<BusinessDayOverrideJpaEntity, UUID> {

  Optional<BusinessDayOverrideJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  // TENANT-LEVEL only (outlet_id IS NULL). Outlet-level rows are owned by core.outlet.
  Optional<BusinessDayOverrideJpaEntity>
      findByTenantIdAndOutletIdIsNullAndBusinessDateAndDeletedAtIsNull(
          UUID tenantId, LocalDate businessDate);

  List<BusinessDayOverrideJpaEntity>
      findByTenantIdAndOutletIdIsNullAndBusinessDateBetweenAndDeletedAtIsNullOrderByBusinessDateAsc(
          UUID tenantId, LocalDate from, LocalDate to);
}
