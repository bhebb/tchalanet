package com.tchalanet.server.core.game.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(path = "tenant-games", collectionResourceRel = "tenantGames")
public interface TenantGameJpaRepository extends JpaRepository<TenantGameJpaEntity, UUID> {

  List<TenantGameJpaEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc();

  List<TenantGameJpaEntity> findByEnabledTrueAndDeletedAtIsNullOrderByUpdatedAtDesc();

  Optional<TenantGameJpaEntity> findByGame_CodeAndDeletedAtIsNull(String code);

  Optional<TenantGameJpaEntity> findByGame_IdAndDeletedAtIsNull(UUID gameId);
}
