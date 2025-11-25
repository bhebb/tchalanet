package com.tchalanet.server.game.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameJpaRepository extends JpaRepository<GameJpaEntity, UUID> {
  List<GameJpaEntity> findByActiveTrueOrderBySortOrder();

  java.util.Optional<GameJpaEntity> findByCode(String code);
}
