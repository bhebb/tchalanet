package com.tchalanet.server.features.reporting.tenantkpis;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetTenantKpisHandler {

  private final GetTenantKpisRepository getTenantKpisRepository;
  private final Clock clock;
  private final TchRequestContextHolder contextHolder;

  public KpisResponse handle(GetTenantKpisQuery query) {
    LocalDate today = LocalDate.now(clock);
    LocalDate from = today.minusDays(6); // 7 jours glissants
    LocalDate to = today.withDayOfMonth(1);

      var kpis = getTenantKpisRepository.computeTenantKpis(contextHolder.get().tenantUuid(), from, to);
      var snapshot = new GetTenantKpisSnapshotDto(from, to, kpis);
      return new KpisResponse(snapshot);
    }

}

