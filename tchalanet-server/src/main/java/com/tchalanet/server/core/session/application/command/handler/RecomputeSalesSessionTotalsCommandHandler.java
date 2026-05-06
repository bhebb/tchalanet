package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.RecomputeSalesSessionTotalsCommand;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionTotalsAggregatePort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionTotalsWriterPort;
import com.tchalanet.server.core.session.domain.model.SalesSessionTotals;
import java.math.BigDecimal;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RecomputeSalesSessionTotalsCommandHandler
    implements CommandHandler<RecomputeSalesSessionTotalsCommand, SalesSessionTotals> {

  private final SalesSessionReaderPort sessionRepo; // pour vérifier existence + scope
  private final SalesSessionTotalsAggregatePort aggregatePort;
  private final SalesSessionTotalsWriterPort totalsWriter;
  private final Clock clock;

  @Override
  @TchTx
  public SalesSessionTotals handle(RecomputeSalesSessionTotalsCommand cmd) {
    var session =
        sessionRepo
            .findById(cmd.sessionId())
            .orElseThrow(() -> new IllegalStateException("Session not found: " + cmd.sessionId()));

    if (!cmd.tenantId().equals(session.tenantId())) {
      throw new SecurityException("Wrong tenant scope");
    }

    var agg = aggregatePort.compute(cmd.tenantId(), cmd.sessionId());

    var stake = nz(agg.totalStake());
    var payout = nz(agg.totalPayout());
    var gross = stake.subtract(payout);
    var now = java.time.Instant.now(clock);

    var totals =
        new SalesSessionTotals(
            cmd.sessionId(), cmd.tenantId(), agg.totalTickets(), stake, payout, gross, now);
    return totalsWriter.upsert(totals);
  }

  private static BigDecimal nz(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }
}
