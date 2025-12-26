package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.RecomputePosSessionTotalsCommand;
import com.tchalanet.server.core.session.application.port.out.PosSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.PosSessionTotalsAggregatePort;
import com.tchalanet.server.core.session.application.port.out.PosSessionTotalsWriterPort;
import com.tchalanet.server.core.session.domain.model.PosSessionTotals;
import java.math.BigDecimal;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RecomputePosSessionTotalsCommandHandler
    implements CommandHandler<RecomputePosSessionTotalsCommand, PosSessionTotals> {

  private final PosSessionReaderPort sessionRepo; // pour vérifier existence + scope
  private final PosSessionTotalsAggregatePort aggregatePort;
  private final PosSessionTotalsWriterPort totalsWriter;
  private final Clock clock;

  @Override
  @TchTx
  public PosSessionTotals handle(RecomputePosSessionTotalsCommand cmd) {
    var session =
        sessionRepo
            .findById(cmd.sessionId().value())
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
        new PosSessionTotals(
            cmd.sessionId(), cmd.tenantId(), agg.totalTickets(), stake, payout, gross, now);
    return totalsWriter.upsert(totals);
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
