package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.common.domain.TenantGameId;
import com.tchalanet.server.common.domain.TenantId;
import com.tchalanet.server.tenant.domain.model.TenantGame;

public final class TenantGameMapper {
  public static TenantGame toDomain(TenantGameJpaEntity e) {
    if (e == null) return null;
    return TenantGame.builder()
        .id(e.getId() == null ? null : new TenantGameId(e.getId()))
        .tenantId(e.getTenantId() == null ? null : TenantId.of(e.getTenantId()))
        .gameId(e.getGameId())
        .enabled(e.getEnabled())
        .displayName(e.getDisplayName())
        .minStake(e.getMinStake())
        .maxStake(e.getMaxStake())
        .flags(e.getFlags())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }

  public static TenantGameJpaEntity toEntity(TenantGame d) {
    if (d == null) return null;
    TenantGameJpaEntity e = new TenantGameJpaEntity();
    if (d.getId() != null && d.getId().value() != null) e.setId(d.getId().value());
    if (d.getTenantId() != null && d.getTenantId().value() != null)
      e.setTenantId(d.getTenantId().value());
    e.setGameId(d.getGameId());
    e.setEnabled(d.getEnabled() == null ? Boolean.TRUE : d.getEnabled());
    e.setDisplayName(d.getDisplayName());
    e.setMinStake(d.getMinStake());
    e.setMaxStake(d.getMaxStake());
    e.setFlags(d.getFlags());
    return e;
  }
}
