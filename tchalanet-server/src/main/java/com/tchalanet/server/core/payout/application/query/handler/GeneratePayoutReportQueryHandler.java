package com.tchalanet.server.core.payout.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.application.query.model.GeneratePayoutReportQuery;
import com.tchalanet.server.core.payout.application.port.out.PayoutReportPort;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GeneratePayoutReportQueryHandler implements QueryHandler<GeneratePayoutReportQuery, Path> {

  private final PayoutReportPort reportPort;

  @Override
  public Path handle(GeneratePayoutReportQuery query) {
    return reportPort.generate(query);
  }
}

