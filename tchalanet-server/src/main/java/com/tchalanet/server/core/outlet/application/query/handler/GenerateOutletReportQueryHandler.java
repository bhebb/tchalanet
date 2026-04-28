package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.port.out.OutletReportPort;
import com.tchalanet.server.core.outlet.application.query.model.GenerateOutletReportQuery;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GenerateOutletReportQueryHandler
    implements QueryHandler<GenerateOutletReportQuery, Path> {

  private final OutletReportPort reportPort;

  @Override
  public Path handle(GenerateOutletReportQuery query) {
    return reportPort.generateReport(query.outletId(), query.from(), query.to());
  }
}
