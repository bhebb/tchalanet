package com.tchalanet.server.core.sales.application.sell;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.command.model.OverrideTicketResultCommand;
import com.tchalanet.server.core.sales.application.command.model.TicketWorkflowResult;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class OverrideTicketResultCommandHandler implements CommandHandler<OverrideTicketResultCommand, TicketWorkflowResult> {

  private final TicketReaderPort reader;
  private final TicketWriterPort writer;

  @Override
  @TchTx
  public TicketWorkflowResult handle(OverrideTicketResultCommand cmd) {
    var ticket = reader.findById(cmd.ticketId())
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + cmd.ticketId()));

    // TODO implement domain transition: override result
    // Important:
    // - never approve/reject by request parameter actor; actor comes from @CurrentContext.
    // - override must block PAID_OUT unless explicit reversal workflow exists.
    // - publish corresponding event AfterCommit when transition is implemented.

    return new TicketWorkflowResult(writer.save(ticket));
  }
}
