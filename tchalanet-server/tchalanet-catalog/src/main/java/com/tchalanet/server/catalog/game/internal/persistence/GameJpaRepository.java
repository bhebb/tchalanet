package com.tchalanet.server.catalog.game.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameJpaRepository extends JpaRepository<GameJpaEntity, UUID> {
  List<GameJpaEntity> findByActiveTrueAndDeletedAtIsNull();

  @Query("SELECT g FROM GameJpaEntity g WHERE g.deletedAt IS NULL " +
         "AND (:active IS NULL OR g.active = :active) " +
         "AND (:q IS NULL OR LOWER(g.name) LIKE :q OR LOWER(g.code) LIKE :q)")
  Page<GameJpaEntity> searchLive(@Param("active") Boolean active, @Param("q") String q, Pageable pageable);

  Optional<GameJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  Optional<GameJpaEntity> findByCodeAndDeletedAtIsNull(String code);

  // NEW: counts
  @Query("select count(g) from GameJpaEntity g where g.deletedAt is null")
  long countAllLive();

  @Query("select count(g) from GameJpaEntity g where g.deletedAt is null and g.active = true")
  long countActiveLive();

  // NEW: recent list (limit handled by caller via Pageable or stream)
  List<GameJpaEntity> findTop10ByDeletedAtIsNullOrderByUpdatedAtDesc();
}
