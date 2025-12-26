package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.command.model.PrintTicketCommand;
import com.tchalanet.server.core.sales.application.port.out.PrintTicketModels;
import com.tchalanet.server.core.sales.application.port.out.TicketPrinterPort;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PrintTicketCommandHandler implements CommandHandler<PrintTicketCommand, String> {

  private final TicketReaderPort ticketRepository;
  private final TicketPrinterPort ticketPrinterPort;
  private TchRequestContextHolder contextHolder;

  @Override
  public String handle(PrintTicketCommand cmd) {
    Ticket ticket =
        ticketRepository
            .findWithLinesById(cmd.ticketId())
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + cmd.ticketId()));

    PrintTicketModels.PrintTicketPayload payload = buildPayload(ticket);

    return ticketPrinterPort.render(payload);
  }

  private PrintTicketModels.PrintTicketPayload buildPayload(Ticket ticket) {
    String terminalInfo = ticket.getTerminalId().toString();
    String drawName = ticket.getDrawId().toString();

    var lines =
        ticket.getLines().stream()
            .map(
                line ->
                    new PrintTicketModels.PrintTicketPayload.Line(
                        line.gameCode(), line.selection(), line.stake(), line.potentialPayout()))
            .collect(Collectors.toList());

    return new PrintTicketModels.PrintTicketPayload(
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        terminalInfo,
        drawName,
        ticket.getCreatedAt(),
        lines,
        ticket.getTotalAmount());
  }
}
