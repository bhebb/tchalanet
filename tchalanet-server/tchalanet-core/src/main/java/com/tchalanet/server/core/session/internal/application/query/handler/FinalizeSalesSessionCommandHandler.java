package com.tchalanet.server.core.session.internal.application.query.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.application.query.model.FinalizeSalesSessionCommand;
import com.tchalanet.server.core.session.domain.model.SalesSessionStatus;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FinalizeSalesSessionCommandHandler
    implements CommandHandler<FinalizeSalesSessionCommand, Void> {

  private final SalesSessionReaderPort reader;
  private final SalesSessionWriterPort writer;
  private final Clock clock;

  @Override
  @TchTx
  public Void handle(FinalizeSalesSessionCommand cmd) {
    var session = reader.getById(cmd.tenantId(), cmd.salesSessionId());

    if (session.status() != SalesSessionStatus.CLOSED) {
      throw ProblemRest.conflict("sales_session.must_be_closed_before_finalize");
    }

    writer.finalizeSession(cmd.salesSessionId(), clock.instant(), cmd.performedBy(), cmd.reason());
    return null;
  }
}
