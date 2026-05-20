package com.tchalanet.server.features.reporting.outletperformance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutletPerformanceReportService {

  private final OutletPerformanceReader outletPerformanceReader;

  public OutletPerformanceReportResponse getReport(OutletPerformanceReportCriteria criteria) {

    var snapshot =
        outletPerformanceReader.findOutletPerformance(
            criteria.tenantId(), criteria.fromDate(), criteria.toDate(), criteria.gameCode());

    var lines =
        snapshot.stream()
            .map(
                line ->
                    new OutletPerformanceLine(
                        line.outletId(),
                        line.outletCode(),
                        line.outletName(),
                        line.gameCode(),
                        line.ticketsSold(),
                        line.totalSales(),
                        line.totalPayout(),
                        line.netRevenue()))
            .toList();

    return new OutletPerformanceReportResponse(
        criteria.fromDate(), criteria.toDate(), criteria.gameCode(), lines);
  }
}
