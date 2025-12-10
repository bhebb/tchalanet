package com.tchalanet.server.core.tenant.infra.adapter;

import com.tchalanet.server.core.tenant.application.port.out.ReportGeneratorPort;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubReportGeneratorAdapter implements ReportGeneratorPort {

  @Override
  public String generateTenantMonthlyReport(UUID tenantId, YearMonth month) {
    // stub: return a fake path
    return "/tmp/report-" + tenantId + "-" + month.toString() + ".pdf";
  }
}

