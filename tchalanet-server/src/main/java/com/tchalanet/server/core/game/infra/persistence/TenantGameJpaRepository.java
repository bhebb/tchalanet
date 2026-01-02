package com.tchalanet.server.core.game.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = true, path = "tenant-games")
public interface TenantGameJpaRepository extends JpaRepository<TenantGameJpaEntity, UUID> {

  Optional<TenantGameJpaEntity> findByGame_CodeAndDeletedAtIsNull(String code);

  Optional<TenantGameJpaEntity> findByGame_IdAndDeletedAtIsNull(UUID gameId);
}
