package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.DrawChannelQueryPort;
import com.tchalanet.server.core.draw.application.query.projection.DrawChannelCalendarRow;
import com.tchalanet.server.core.draw.infra.persistence.entity.DrawChannelJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawChannelJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DrawChannelQueryJpaAdapter implements DrawChannelQueryPort {

  private final DrawChannelJpaRepository repo;

  @Override
  public List<DrawChannelCalendarRow> listActiveCalendarRows(UUID tenantId) {
    return repo.findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(tenantId).stream()
        .map(this::toRow)
        .toList();
  }

  private DrawChannelCalendarRow toRow(DrawChannelJpaEntity e) {
    return new DrawChannelCalendarRow(
        e.getId(),
        e.getTenantGameId(),
        e.getCode(),
        e.getTimezone(),
        e.getDrawTime(),
        e.getCutoffSec() == null ? 0 : e.getCutoffSec(),
        e.getDaysOfWeek(),
        null,
        Boolean.TRUE.equals(e.getActive()),
        e.getSortOrder() == null ? 0 : e.getSortOrder());
  }
}
