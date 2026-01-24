package com.tchalanet.server.catalog.game.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameJpaRepository extends JpaRepository<GameJpaEntity, UUID> {
  List<GameJpaEntity> findByActiveTrueAndDeletedAtIsNull();

  Optional<GameJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  Optional<GameJpaEntity> findByCodeAndDeletedAtIsNull(String code);
}
