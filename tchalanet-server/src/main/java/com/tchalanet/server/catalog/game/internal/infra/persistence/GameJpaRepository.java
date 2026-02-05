package com.tchalanet.server.catalog.game.internal.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(exported = true, path = "games", collectionResourceRel = "games")
public interface GameJpaRepository extends JpaRepository<GameJpaEntity, UUID> {
  List<GameJpaEntity> findByActiveTrueOrderBySortOrder();

  List<GameJpaEntity> findAllByOrderBySortOrderAsc();

  Optional<GameJpaEntity> findByCode(String code);

  List<GameJpaEntity> findByOrderByUpdatedAtDesc(Pageable pageable);

  long countByDeletedAtIsNull();

  long countByActiveTrueAndDeletedAtIsNull();
}
