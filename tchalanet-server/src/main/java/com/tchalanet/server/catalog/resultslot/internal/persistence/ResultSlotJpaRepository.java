package com.tchalanet.server.catalog.resultslot.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultSlotJpaRepository extends JpaRepository<ResultSlotJpaEntity, UUID> {

    Optional<ResultSlotJpaEntity> findFirstBySlotKeyIgnoreCaseAndDeletedAtIsNull(String slotKey);

    List<ResultSlotJpaEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

    Optional<ResultSlotJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    long countByDeletedAtIsNull();

    long countByActiveTrueAndDeletedAtIsNull();
}
