package com.tchalanet.server.features.reporting.salesreport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSalesReportByPeriodAndGameHandler {

  private final SalesReportRepository salesReportRepository;

  public SalesReportResponse handle(GetSalesReportByPeriodAndGameQuery query) {

    var snapshot = salesReportRepository.findSalesByPeriodAndGame(query);

    return new SalesReportResponse(query.fromDate(), query.toDate(), query.gameCode(), snapshot);
  }
}
