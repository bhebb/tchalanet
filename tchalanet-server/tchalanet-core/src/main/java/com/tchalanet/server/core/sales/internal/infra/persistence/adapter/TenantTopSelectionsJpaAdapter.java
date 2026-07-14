package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.query.TenantTopSelectionsByPeriodView;
import com.tchalanet.server.core.sales.internal.application.port.out.TenantTopSelectionsReaderPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantTopSelectionsJpaAdapter implements TenantTopSelectionsReaderPort {

  private final TicketJpaRepository ticketRepository;

  @Override
  public TenantTopSelectionsByPeriodView findTopSelectionsByPeriod(
      TenantId tenantId, LocalDate from, LocalDate to, ZoneId zoneId, int limit) {
    ZoneId effectiveZoneId = zoneId != null ? zoneId : ZoneId.systemDefault();
    var rows =
        ticketRepository.topSelectionsByTenantAndPeriod(
            tenantId.value(),
            from.atStartOfDay(effectiveZoneId).toInstant(),
            to.plusDays(1).atStartOfDay(effectiveZoneId).toInstant(),
            limit);
    var items = rows.stream().map(this::selectionItem).toList();
    return new TenantTopSelectionsByPeriodView(from, to, items);
  }

  private TenantTopSelectionsByPeriodView.SelectionItem selectionItem(Object[] row) {
    return new TenantTopSelectionsByPeriodView.SelectionItem(
        ((Number) row[0]).intValue(),
        (String) row[1],
        (String) row[2],
        (String) row[3],
        row[4] == null ? null : ((Number) row[4]).shortValue(),
        ((Number) row[5]).longValue(),
        (BigDecimal) row[6]);
  }
}
