package com.tchalanet.server.core.sales.infra.printer;

import com.tchalanet.server.core.sales.domain.ports.in.PrintTicketUseCase;
import com.tchalanet.server.core.sales.domain.ports.in.PrintTicketUseCase.PrintTicketPayload;
import com.tchalanet.server.core.sales.domain.ports.out.TicketPrinterPort;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class PlainTextTicketPrinterAdapter implements TicketPrinterPort {

  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  @Override
  public String render(PrintTicketPayload payload) {
    StringBuilder sb = new StringBuilder();
    sb.append("****************************************\n");
    sb.append("         ").append(payload.tenantName()).append("\n");
    sb.append("****************************************\n");
    sb.append("Terminal: ").append(payload.terminalInfo()).append("\n");
    sb.append("Ticket #: ").append(payload.ticketCode()).append("\n");
    sb.append("Date: ").append(dtf.format(payload.createdAt())).append("\n");
    sb.append("\n");
    sb.append("Draw: ").append(payload.drawName()).append("\n");
    sb.append("Draw Time: ").append(dtf.format(payload.drawTime())).append("\n");
    sb.append("----------------------------------------\n");

    for (PrintTicketUseCase.PrintLine line : payload.lines()) {
      sb.append(
          String.format(
              "%-10s %-10s %6.2f -> %8.2f\n",
              line.gameLabel(), line.selection(), line.stake(), line.potentialPayout()));
    }

    sb.append("----------------------------------------\n");
    sb.append(String.format("TOTAL: %28.2f\n", payload.totalAmount()));
    sb.append("\n");
    sb.append("Verify code: ").append(payload.publicCode()).append("\n");
    sb.append("****************************************\n");

    return sb.toString();
  }
}
