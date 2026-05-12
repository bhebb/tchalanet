package com.tchalanet.server.features.reporting.salesreport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalesReportService {

  private final SalesReportReader salesReportReader;

  public SalesReportResponse getReport(SalesReportCriteria criteria) {

    var snapshot = salesReportReader.findSalesByPeriodAndGame(criteria);

    return new SalesReportResponse(
        criteria.fromDate(), criteria.toDate(), criteria.gameCode(), snapshot);
  }
}
