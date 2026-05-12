package com.tchalanet.server.features.reporting.tenantkpis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service pour récupérer les KPIs (indicateurs clés) d'un tenant. */
@Service
@RequiredArgsConstructor
public class GetTenantKpisService {

  private final TenantKpisReader tenantKpisReader;

  public KpisResponse getKpis(TenantKpisCriteria criteria) {

    var snapshot =
        tenantKpisReader.computeTenantKpis(
            criteria.tenantId(), criteria.fromDate(), criteria.toDate());

    var kpisView =
        new KpisView(
            snapshot.ticketsSold(),
            snapshot.totalSales(),
            snapshot.totalPayout(),
            snapshot.netRevenue(),
            snapshot.activeOutlets(),
            snapshot.activeCashiers(),
            null,
            null,
            0,
            null);
    var snapshotView = new GetTenantKpisSnapshot(criteria.fromDate(), criteria.toDate(), kpisView);

    return new KpisResponse(snapshotView);
  }
}
