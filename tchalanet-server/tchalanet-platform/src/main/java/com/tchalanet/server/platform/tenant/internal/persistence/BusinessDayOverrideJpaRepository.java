package com.tchalanet.server.platform.tenant.internal.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessDayOverrideJpaRepository
    extends JpaRepository<BusinessDayOverrideJpaEntity, UUID> {

  Optional<BusinessDayOverrideJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  Optional<BusinessDayOverrideJpaEntity>
      findByTenantIdAndBusinessDateAndDeletedAtIsNull(
          UUID tenantId, LocalDate businessDate);

  List<BusinessDayOverrideJpaEntity>
      findByTenantIdAndBusinessDateBetweenAndDeletedAtIsNullOrderByBusinessDateAsc(
          UUID tenantId, LocalDate from, LocalDate to);
}
