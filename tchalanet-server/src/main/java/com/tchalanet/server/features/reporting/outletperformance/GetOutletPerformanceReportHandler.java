package com.tchalanet.server.features.reporting.outletperformance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOutletPerformanceReportHandler {

  private final OutletPerformanceRepository outletPerformanceRepository;

  public OutletPerformanceReportResponse handle(GetOutletPerformanceReportQuery query) {

    var snapshot =
        outletPerformanceRepository.findOutletPerformance(
            query.tenantId(), query.fromDate(), query.toDate(), query.gameCode());

    var lines =
        snapshot.stream()
            .map(
                line ->
                    new OutletPerformanceLineDto(
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
        query.fromDate(), query.toDate(), query.gameCode(), lines);
  }
}
