package com.tchalanet.server.catalog.resultslot.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultSlotCalendarOverrideJpaRepository
    extends JpaRepository<ResultSlotCalendarOverrideJpaEntity, UUID> {

  List<ResultSlotCalendarOverrideJpaEntity>
      findByResultSlotIdAndDeletedAtIsNullOrderBySlotLocalDateAscRecurringMdAsc(UUID resultSlotId);

  Optional<ResultSlotCalendarOverrideJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
