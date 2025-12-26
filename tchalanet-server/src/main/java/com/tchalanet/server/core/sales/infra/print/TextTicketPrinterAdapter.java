package com.tchalanet.server.core.sales.infra.print;

import com.tchalanet.server.core.sales.application.port.out.PrintTicketModels;
import com.tchalanet.server.core.sales.application.port.out.TicketPrinterPort;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TextTicketPrinterAdapter implements TicketPrinterPort {

  @Override
  public String render(PrintTicketModels.PrintTicketPayload payload) {
    var lines =
        payload.lines().stream()
            .map(
                l ->
                    "- "
                        + l.gameCode()
                        + " "
                        + l.selection()
                        + " stake="
                        + l.stake()
                        + " odds="
                        + l.oddsSnapshot())
            .collect(Collectors.joining("\n"));

    return """
            TCHALANET - TICKET
            Ticket: %s
            Verify: %s
            Terminal: %s
            Draw: %s
            Date: %s
            -------------------------
            %s
            -------------------------
            TOTAL: %s
            """
        .formatted(
            payload.ticketCode(),
            payload.publicCode(),
            payload.terminalId(),
            payload.drawId(),
            payload.createdAt(),
            lines,
            payload.totalAmount());
  }
}
