package com.tchalanet.server.core.ledger.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.query.model.GenerateLedgerReportQuery;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GenerateLedgerReportQueryHandler implements QueryHandler<GenerateLedgerReportQuery, Path> {

  @Override
  public Path handle(GenerateLedgerReportQuery query) {
    // TODO: implement report generation
    throw new UnsupportedOperationException("GenerateLedgerReportQueryHandler not implemented yet");
  }
}

