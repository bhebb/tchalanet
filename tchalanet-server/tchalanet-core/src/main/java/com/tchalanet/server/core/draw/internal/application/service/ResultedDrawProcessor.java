package com.tchalanet.server.core.draw.internal.application.service;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.command.SettleDrawCommand;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.sales.api.command.result.RecordDrawTicketsResultCommand;
import com.tchalanet.server.core.sales.internal.application.query.model.CountPendingTicketsByDrawIdQuery;
import com.tchalanet.server.core.sales.internal.application.query.model.ExistsPendingTicketsByDrawIdQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Processes pending ticket results before transitioning an eligible draw to {@code SETTLED}. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class ResultedDrawProcessor {

  private static final int RESULT_TICKET_BATCH_SIZE = 250;

  private final DrawLookupPort drawLookupPort;
  private final DrawSummaryReaderPort drawSummaryReaderPort;
  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public boolean process(DrawId drawId) {
    var draw = drawLookupPort.findById(drawId).orElse(null);
    if (draw == null || draw.status() != DrawStatus.RESULTED) {
      return false;
    }
    if (draw.drawResultId() == null) {
      log.warn("draw.resulted-process.skip draw={} tenant={} reason=no_result", drawId, draw.tenantId());
      return false;
    }

    var summary = drawSummaryReaderPort.getById(drawId);
    if (summary.result() == null || summary.resultSlotId() == null) {
      log.warn(
          "draw.resulted-process.skip draw={} tenant={} reason=incomplete_result_summary",
          drawId,
          draw.tenantId());
      return false;
    }

    var ticketOutcome =
        commandBus.execute(
            new RecordDrawTicketsResultCommand(
                draw.tenantId(),
                draw.id(),
                summary.result().id(),
                summary.drawDate(),
                summary.resultSlotId(),
                summary.drawChannelId(),
                RESULT_TICKET_BATCH_SIZE));
    if (!ticketOutcome.complete()) {
      log.info(
          "draw.resulted-process.defer draw={} tenant={} remainingTickets={} failedTickets={}",
          drawId,
          draw.tenantId(),
          ticketOutcome.remainingTickets(),
          ticketOutcome.failedTickets());
      return false;
    }

    if (queryBus.ask(new ExistsPendingTicketsByDrawIdQuery(draw.id()))) {
      long pending = queryBus.ask(new CountPendingTicketsByDrawIdQuery(draw.id()));
      log.info("draw.resulted-process.defer draw={} tenant={} pendingTickets={}", drawId, draw.tenantId(), pending);
      return false;
    }

    commandBus.execute(new SettleDrawCommand(List.of(drawId), null, false));
    return true;
  }
}
