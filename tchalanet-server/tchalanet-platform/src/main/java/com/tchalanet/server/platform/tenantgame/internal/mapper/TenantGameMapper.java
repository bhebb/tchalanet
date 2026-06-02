package com.tchalanet.server.platform.tenantgame.internal.mapper;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGameJpaEntity;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGame;
import org.springframework.stereotype.Component;

@Component
public class TenantGameMapper {

    public TenantGame toDomain(TenantGameJpaEntity entity) {
        return new TenantGame(
            TenantGameId.nullableOf(entity.getId()),
            TenantId.nullableOf(entity.getTenantId()),
            GameId.nullableOf(entity.getGameId()),
            entity.getGameCode(),
            entity.isEnabled(),
            entity.isVisibleInPos(),
            entity.getDisplayName(),
            entity.getDisplayOrder(),
            entity.getMinStake(),
            entity.getMaxStake(),
            entity.isAvailabilityEnabled(),
            entity.getAvailabilityDays(),
            entity.getStartLocalTime(),
            entity.getEndLocalTime());
    }

    public TenantGameJpaEntity toEntity(TenantGame domain) {
        var entity = new TenantGameJpaEntity();
        updateEntityFromDomain(domain, entity);
        return entity;
    }

    public void updateEntityFromDomain(TenantGame domain, TenantGameJpaEntity entity) {
        if (domain.gameId() != null) entity.setGameId(domain.gameId().value());
        if (domain.gameCode() != null) entity.setGameCode(domain.gameCode());
        entity.setEnabled(domain.enabled());
        entity.setVisibleInPos(domain.visibleInPos());
        entity.setDisplayName(domain.displayName());
        entity.setDisplayOrder(domain.displayOrder());
        entity.setMinStake(domain.minStake());
        entity.setMaxStake(domain.maxStake());
        entity.setAvailabilityEnabled(domain.availabilityEnabled());
        entity.setAvailabilityDays(domain.availabilityDays());
        entity.setStartLocalTime(domain.startLocalTime());
        entity.setEndLocalTime(domain.endLocalTime());
    }
}
