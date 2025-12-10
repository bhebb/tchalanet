package com.tchalanet.server.core.tenant.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.query.model.GenerateTenantPerPeriodReportQuery;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GenerateTenanPerPeriodReportQueryHandler implements QueryHandler<GenerateTenantPerPeriodReportQuery, Path> {

  @Override
  public Path handle(GenerateTenantPerPeriodReportQuery query) {
    // TODO: generate PDF/Excel report and return path
    throw new UnsupportedOperationException("GenerateTenantMonthlyReportQueryHandler not implemented yet");
  }
}
