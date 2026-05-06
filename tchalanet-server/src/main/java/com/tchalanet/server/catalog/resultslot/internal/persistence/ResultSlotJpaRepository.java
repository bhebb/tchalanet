package com.tchalanet.server.catalog.resultslot.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ResultSlotJpaRepository extends JpaRepository<ResultSlotJpaEntity, UUID> {

    Optional<ResultSlotJpaEntity> findFirstBySlotKeyIgnoreCaseAndDeletedAtIsNull(String slotKey);

    List<ResultSlotJpaEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

    Optional<ResultSlotJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    @Query("select count(r) from ResultSlotJpaEntity r where r.deletedAt is null")
    long countAllLive();

    @Query("select count(r) from ResultSlotJpaEntity r where r.deletedAt is null and r.active = true")
    long countActiveLive();

}
