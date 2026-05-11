package com.tchalanet.server.core.sales.application.sell;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.draw.application.query.model.GetDrawForSaleQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.EvaluateSaleLimitPolicyQuery;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.sales.application.command.model.SyncOfflineSalesCommand;
import com.tchalanet.server.core.sales.application.command.model.SyncOfflineSalesResult;
import com.tchalanet.server.core.sales.application.command.model.SyncOfflineTicketDecision;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.application.service.OfflineSalesGateEvaluator;
import com.tchalanet.server.core.session.application.query.model.GetSalesSessionForSaleQuery;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SyncOfflineSalesCommandHandler
    implements CommandHandler<SyncOfflineSalesCommand, SyncOfflineSalesResult> {

  private final QueryBus queryBus;
  private final TicketReaderPort ticketReader;
  private final TicketWriterPort ticketWriter;
  private final OfflineSalesGateEvaluator gateEvaluator;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public SyncOfflineSalesResult handle(SyncOfflineSalesCommand command) {
    var decisions = new ArrayList<SyncOfflineTicketDecision>();

    for (var input : command.tickets()) {
      if (ticketReader.existsAcceptedOfflineCode(input.offlineCode())) {
        decisions.add(new SyncOfflineTicketDecision(
            input.submissionId(), SalesOfflineDecision.CONFLICT, null, null));
        continue;
      }

      var session = queryBus.ask(new GetSalesSessionForSaleQuery(input.salesSessionId()));
      var draw = queryBus.ask(new GetDrawForSaleQuery(input.drawId()));
      var limit = queryBus.ask(new EvaluateSaleLimitPolicyQuery(
          input.outletId(),
          input.terminalId(),
          input.sellerUserId(),
          input.stakeAmount(),
          input.totalAmount()));

      var gate = gateEvaluator.evaluate(input, draw, session, limit);

      if (!gate.accepted()) {
        decisions.add(new SyncOfflineTicketDecision(
            input.submissionId(), gate.decision(), gate.rejectReason(), null));
        continue;
      }

      // TODO: create real Sales Ticket, save, publish TicketPlacedEvent after commit.
      var ticketId = TicketId.of(idGenerator.newUuid());

      decisions.add(new SyncOfflineTicketDecision(
          input.submissionId(), gate.decision(), null, ticketId));
    }

    int accepted = (int) decisions.stream()
        .filter(d -> d.decision() == SalesOfflineDecision.ACCEPTED
            || d.decision() == SalesOfflineDecision.ACCEPTED_POST_CLOSE_ADJUSTMENT)
        .count();
    int rejected = (int) decisions.stream()
        .filter(d -> d.decision() == SalesOfflineDecision.REJECTED)
        .count();
    int review = (int) decisions.stream()
        .filter(d -> d.decision() == SalesOfflineDecision.REVIEW_REQUIRED)
        .count();
    int conflict = (int) decisions.stream()
        .filter(d -> d.decision() == SalesOfflineDecision.CONFLICT)
        .count();

    return new SyncOfflineSalesResult(accepted, rejected, review, conflict, decisions);
  }
}
