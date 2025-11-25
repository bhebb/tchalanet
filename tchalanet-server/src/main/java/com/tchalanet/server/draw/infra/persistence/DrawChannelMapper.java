package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.common.domain.DrawChannelId;
import com.tchalanet.server.common.domain.TenantId;
import com.tchalanet.server.draw.domain.model.DrawChannel;

public final class DrawChannelMapper {
  public static DrawChannel toDomain(DrawChannelJpaEntity e) {
    if (e == null) return null;
    return DrawChannel.builder()
        .id(e.getId() == null ? null : new DrawChannelId(e.getId()))
        .tenantId(e.getTenantId() == null ? null : new TenantId(e.getTenantId()))
        .code(e.getCode())
        .name(e.getName())
        .gameId(e.getGameId())
        // gameCode pourra être résolu plus tard en joignant game si nécessaire
        .timezone(e.getTimezone())
        .drawTime(e.getDrawTime())
        .cutoffSec(e.getCutoffSec())
        .daysOfWeek(e.getDaysOfWeek())
        .active(e.getActive())
        .sortOrder(e.getSortOrder())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }

  public static DrawChannelJpaEntity toEntity(DrawChannel d) {
    if (d == null) return null;
    DrawChannelJpaEntity e = new DrawChannelJpaEntity();
    if (d.getId() != null && d.getId().value() != null) e.setId(d.getId().value());
    if (d.getTenantId() != null && d.getTenantId().value() != null)
      e.setTenantId(d.getTenantId().value());
    e.setCode(d.getCode());
    e.setName(d.getName());
    e.setGameId(d.getGameId());
    e.setTimezone(d.getTimezone());
    e.setDrawTime(d.getDrawTime());
    e.setCutoffSec(d.getCutoffSec());
    e.setDaysOfWeek(d.getDaysOfWeek());
    e.setActive(d.getActive() == null ? Boolean.TRUE : d.getActive());
    e.setSortOrder(d.getSortOrder() == null ? 0 : d.getSortOrder());
    return e;
  }
}
