package com.tchalanet.server.platform.tenantgame.internal.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.internal.mapper.TenantGameMapper;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantGamePersistenceAdapter {

    private final TenantGameJpaRepository repository;
    private final TenantGameMapper mapper;

    public TenantGame save(TenantGame tenantGame) {
        var existing = tenantGame.gameId() != null
            ? repository.findByTenantIdAndGameId(tenantGame.tenantId().value(), tenantGame.gameId().value())
            : Optional.<TenantGameJpaEntity>empty();

        TenantGameJpaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            mapper.updateEntityFromDomain(tenantGame, entity);
        } else {
            entity = mapper.toEntity(tenantGame);
        }
        return mapper.toDomain(repository.save(entity));
    }

    public Optional<TenantGame> findByTenantIdAndGameCode(TenantId tenantId, String gameCode) {
        return repository.findByTenantIdAndGameCode(tenantId.value(), gameCode.toUpperCase())
            .map(mapper::toDomain);
    }

    public List<TenantGame> findAllByTenantId(TenantId tenantId) {
        return repository.findByTenantId(tenantId.value()).stream()
            .map(mapper::toDomain)
            .toList();
    }

    public List<TenantGame> findEnabledByTenantId(TenantId tenantId) {
        return repository.findByTenantIdAndEnabled(tenantId.value(), true).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
