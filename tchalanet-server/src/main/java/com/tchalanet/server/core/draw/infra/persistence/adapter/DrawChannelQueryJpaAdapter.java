package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelQueryPort;
import com.tchalanet.server.core.draw.application.query.projection.DrawChannelCalendarRow;
import com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawChannelJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawChannelQueryJpaAdapter implements DrawChannelQueryPort {

  private final DrawChannelJpaRepository repo;

  @Override
  public List<DrawChannelCalendarRow> listActiveCalendarRows(TenantId tenantId) {
    return repo
        .findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(tenantId.uuid())
        .stream()
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
