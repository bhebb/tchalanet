package com.tchalanet.server.features.reporting.tenantkpis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use case pour récupérer les KPIs (indicateurs clés) d'un tenant.
 */
@Service
@RequiredArgsConstructor
public class GetTenantKpisUseCase {

    private final GetTenantKpisRepository getTenantKpisRepository;

    public KpisResponse getKpis(GetTenantKpisQuery query) {

        var snapshot = getTenantKpisRepository.computeTenantKpis(query.tenantId(),
            query.fromDate(),
            query.toDate());

        var kpisDto = new KpisDto(
            snapshot.ticketsSold(),
            snapshot.totalSales(),
            snapshot.totalPayout(),
            snapshot.netRevenue(),
            snapshot.activeOutlets(),
            snapshot.activeCashiers(),
            null,
            null,
            0,
            null
        );
        var snapshotDto = new GetTenantKpisSnapshotDto(query.fromDate(), query.toDate(), kpisDto);

        return new KpisResponse(snapshotDto);
    }
}
