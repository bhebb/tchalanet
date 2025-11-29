package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.core.tenant.infra.persistence.entity.GameEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameJpaRepository extends JpaRepository<GameEntity, UUID> {
  Optional<GameEntity> findByCode(String code);
}
