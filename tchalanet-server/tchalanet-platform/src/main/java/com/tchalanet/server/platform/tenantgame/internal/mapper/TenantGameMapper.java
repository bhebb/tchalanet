package com.tchalanet.server.platform.tenantgame.internal.mapper;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGameJpaEntity;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGame;
import org.springframework.stereotype.Component;

@Component
public class TenantGameMapper {

  public TenantGame toDomain(TenantGameJpaEntity entity) {
    return new TenantGame(
        TenantGameId.nullableOf(entity.getId()),
        com.tchalanet.server.common.types.id.TenantId.nullableOf(entity.getTenantId()),
        GameId.nullableOf(entity.getGameId()),
        null,
        null,
        null,
        null,
        null,
        null,
        entity.isEnabled(),
        entity.getDisplayName(),
        entity.getMinStake(),
        entity.getMaxStake(),
        entity.getFlags());
  }

  public TenantGameJpaEntity toEntity(TenantGame domain) {
    var entity = new TenantGameJpaEntity();
    updateEntityFromDomain(domain, entity);
    return entity;
  }

  public void updateEntityFromDomain(TenantGame domain, TenantGameJpaEntity entity) {
    if (domain.gameId() != null) {
      entity.setGameId(domain.gameId().value());
    }
    entity.setEnabled(Boolean.TRUE.equals(domain.enabled()));
    entity.setDisplayName(domain.displayName());
    entity.setMinStake(domain.minStake());
    entity.setMaxStake(domain.maxStake());
    entity.setFlags(domain.flags());
  }
}

